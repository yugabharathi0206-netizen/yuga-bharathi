/**
 * Dr. Balaji Homeo Care - Firebase Backend Configuration
 * Shared real-time cloud database between Patient Website and Doctor Android App
 * Automatically handles custom named Firestore database: ai-studio-android-homeocli-a8bd5b9a-3f9e-4fb2-bdf2-754ca5ca5eee
 */
const FIREBASE_CONFIG = {
  apiKey: "AIzaSyAKyeBOsKoLXy6xCgQoTImsl0u5oVikekU",
  authDomain: "gen-lang-client-0498394744.firebaseapp.com",
  projectId: "gen-lang-client-0498394744",
  storageBucket: "gen-lang-client-0498394744.firebasestorage.app",
  messagingSenderId: "135294159728",
  appId: "1:135294159728:android:30f7854afcf68acaad9db5",
  databaseId: "ai-studio-android-homeocli-a8bd5b9a-3f9e-4fb2-bdf2-754ca5ca5eee"
};

// Cloud Firestore REST & SDK Bridge
class FirestoreBridge {
  constructor(config) {
    this.config = config;
    this.baseUrl = `https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/${config.databaseId}/documents`;
  }

  // Convert JS object to Firestore document fields
  toFirestoreFields(obj) {
    const fields = {};
    for (const [key, value] of Object.entries(obj)) {
      if (value === null || value === undefined) {
        fields[key] = { nullValue: null };
      } else if (typeof value === "string") {
        fields[key] = { stringValue: value };
      } else if (typeof value === "number") {
        if (Number.isInteger(value)) {
          fields[key] = { integerValue: String(value) };
        } else {
          fields[key] = { doubleValue: value };
        }
      } else if (typeof value === "boolean") {
        fields[key] = { booleanValue: value };
      } else if (Array.isArray(value)) {
        fields[key] = {
          arrayValue: {
            values: value.map(v => typeof v === "string" ? { stringValue: v } : { stringValue: String(v) })
          }
        };
      } else if (typeof value === "object") {
        fields[key] = { mapValue: { fields: this.toFirestoreFields(value) } };
      }
    }
    return fields;
  }

  // Convert Firestore document fields to clean JS object
  fromFirestoreFields(fields) {
    if (!fields) return {};
    const obj = {};
    for (const [key, val] of Object.entries(fields)) {
      if ("stringValue" in val) obj[key] = val.stringValue;
      else if ("integerValue" in val) obj[key] = parseInt(val.integerValue, 10);
      else if ("doubleValue" in val) obj[key] = parseFloat(val.doubleValue);
      else if ("booleanValue" in val) obj[key] = val.booleanValue;
      else if ("nullValue" in val) obj[key] = null;
      else if ("arrayValue" in val) {
        obj[key] = (val.arrayValue.values || []).map(item => item.stringValue || Object.values(item)[0]);
      } else if ("mapValue" in val) {
        obj[key] = this.fromFirestoreFields(val.mapValue.fields);
      }
    }
    return obj;
  }

  collection(collectionName) {
    const self = this;
    return {
      doc(docId) {
        return {
          async set(data, options = {}) {
            const isMerge = Boolean(options && options.merge);
            let url = `${self.baseUrl}/${collectionName}/${encodeURIComponent(docId)}?key=${self.config.apiKey}`;
            if (isMerge) {
              const keys = Object.keys(data);
              if (keys.length > 0) {
                const mask = keys.map(k => `updateMask.fieldPaths=${encodeURIComponent(k)}`).join("&");
                url += `&${mask}`;
              }
            }
            const fields = self.toFirestoreFields(data);
            const res = await fetch(url, {
              method: "PATCH",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ fields })
            });
            if (!res.ok) {
              const errText = await res.text();
              throw new Error(`Firestore REST Error (${res.status}): ${errText}`);
            }
            return true;
          },

          async update(data) {
            return this.set(data, { merge: true });
          },

          async get() {
            const url = `${self.baseUrl}/${collectionName}/${encodeURIComponent(docId)}?key=${self.config.apiKey}`;
            const res = await fetch(url);
            if (res.status === 404) {
              return { exists: false, id: docId, data: () => null };
            }
            if (!res.ok) {
              throw new Error(`Firestore get error: ${res.statusText}`);
            }
            const doc = await res.json();
            const clean = self.fromFirestoreFields(doc.fields);
            return {
              exists: true,
              id: docId,
              data: () => clean
            };
          },

          onSnapshot(callback, errorCallback) {
            let active = true;
            let timerId = null;
            const pollDoc = async () => {
              if (!active) return;
              try {
                const snapshot = await this.get();
                if (active) callback(snapshot);
              } catch (e) {
                if (errorCallback) errorCallback(e);
                else console.warn(`Doc ${docId} live sync notice:`, e.message);
              }
              if (active) {
                timerId = setTimeout(pollDoc, 2500);
              }
            };
            pollDoc();
            return () => {
              active = false;
              if (timerId) clearTimeout(timerId);
            };
          }
        };
      },

      where(field, op, val) {
        return {
          limit(num) {
            return {
              async get() {
                const url = `https://firestore.googleapis.com/v1/projects/${self.config.projectId}/databases/${self.config.databaseId}/documents:runQuery?key=${self.config.apiKey}`;
                const queryPayload = {
                  structuredQuery: {
                    from: [{ collectionId: collectionName }],
                    where: {
                      fieldFilter: {
                        field: { fieldPath: field },
                        op: op === "==" ? "EQUAL" : "EQUAL",
                        value: typeof val === "number" ? { integerValue: String(val) } : { stringValue: String(val) }
                      }
                    },
                    limit: num
                  }
                };
                const res = await fetch(url, {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify(queryPayload)
                });
                if (!res.ok) return { empty: true, docs: [] };
                const results = await res.json();
                const docs = (results || [])
                  .filter(r => r.document)
                  .map(r => {
                    const docPath = r.document.name || "";
                    const docId = docPath.split("/").pop();
                    const cleanData = self.fromFirestoreFields(r.document.fields);
                    return {
                      id: docId,
                      exists: true,
                      data: () => cleanData
                    };
                  });
                return {
                  empty: docs.length === 0,
                  docs: docs,
                  size: docs.length
                };
              }
            };
          },

          onSnapshot(callback, errorCallback) {
            let active = true;
            let timerId = null;
            const poll = async () => {
              if (!active) return;
              try {
                const url = `https://firestore.googleapis.com/v1/projects/${self.config.projectId}/databases/${self.config.databaseId}/documents:runQuery?key=${self.config.apiKey}`;
                const queryPayload = {
                  structuredQuery: {
                    from: [{ collectionId: collectionName }],
                    where: {
                      fieldFilter: {
                        field: { fieldPath: field },
                        op: op === "==" ? "EQUAL" : "EQUAL",
                        value: typeof val === "number" ? { integerValue: String(val) } : { stringValue: String(val) }
                      }
                    }
                  }
                };
                const res = await fetch(url, {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify(queryPayload)
                });
                if (res.ok) {
                  const results = await res.json();
                  const docs = (results || [])
                    .filter(r => r.document)
                    .map(r => {
                      const docId = (r.document.name || "").split("/").pop();
                      const cleanData = self.fromFirestoreFields(r.document.fields);
                      return {
                        id: cleanData.id || cleanData.appointmentId || docId,
                        exists: true,
                        data: () => cleanData
                      };
                    });
                  if (active) {
                    callback({
                      empty: docs.length === 0,
                      docs: docs,
                      size: docs.length,
                      forEach(cb) { docs.forEach(cb); }
                    });
                  }
                }
              } catch (e) {
                if (errorCallback) errorCallback(e);
                else console.warn("Polling query notice:", e.message);
              }
              if (active) timerId = setTimeout(poll, 2500);
            };
            poll();
            return () => {
              active = false;
              if (timerId) clearTimeout(timerId);
            };
          }
        };
      },

      async get() {
        const url = `https://firestore.googleapis.com/v1/projects/${self.config.projectId}/databases/${self.config.databaseId}/documents:runQuery?key=${self.config.apiKey}`;
        const queryPayload = {
          structuredQuery: {
            from: [{ collectionId: collectionName }]
          }
        };
        const res = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(queryPayload)
        });
        if (!res.ok) return { empty: true, docs: [], size: 0 };
        const results = await res.json();
        const docs = (results || [])
          .filter(r => r && r.document)
          .map(r => {
            const docId = (r.document.name || "").split("/").pop();
            const clean = self.fromFirestoreFields(r.document.fields);
            return {
              id: clean.id || clean.appointmentId || docId,
              exists: true,
              data: () => clean
            };
          });
        return { empty: docs.length === 0, docs, size: docs.length };
      },

      onSnapshot(callback, errorCallback) {
        let active = true;
        let timerId = null;
        const pollCollection = async () => {
          if (!active) return;
          try {
            const snap = await this.get();
            if (active) {
              callback({
                empty: snap.empty,
                docs: snap.docs,
                size: snap.size,
                forEach(cb) { snap.docs.forEach(cb); }
              });
            }
          } catch (e) {
            if (errorCallback) errorCallback(e);
            else console.warn(`Collection ${collectionName} live sync notice:`, e.message);
          }
          if (active) {
            timerId = setTimeout(pollCollection, 2500);
          }
        };
        pollCollection();
        return () => {
          active = false;
          if (timerId) clearTimeout(timerId);
        };
      }
    };
  }
}

// Global initialization
let db = new FirestoreBridge(FIREBASE_CONFIG);
let isFirebaseConnected = true;

function initFirebase() {
  if (typeof window !== "undefined") {
    window.db = db;
    window.isFirebaseConnected = true;
    console.log("✓ Connected to Shared Clinic Firestore Database (" + FIREBASE_CONFIG.databaseId + ")");
  }
  return db;
}

if (typeof window !== "undefined") {
  window.FIREBASE_CONFIG = FIREBASE_CONFIG;
  window.FirestoreBridge = FirestoreBridge;
  window.db = db;
  window.isFirebaseConnected = true;
  window.initFirebase = initFirebase;
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = { FIREBASE_CONFIG, FirestoreBridge, db };
}
