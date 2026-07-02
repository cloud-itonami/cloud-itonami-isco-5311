(ns child-care.store
  "SSoT for the ISCO-08 5311 independent child-care-worker
  sole-proprietor actor, behind a `Store` protocol so the backend is a
  swap (MemStore default ‖ a real Datomic/kotoba-server backend, per the
  itonami actor pattern).

  Domain = independent child care practice:

    child             — a registered child (childId, guardianConsent?
                        boolean, allergies — a set of food names)
    activity          — an activity event under a child (activityId,
                        childId, category #{:play :meal :nap :outdoor})
    incident-report   — an incident report under a child (reportId,
                        childId, severity)

  The append-only records are the operating ledger: an activity or
  incident report must reference a registered (guardian-consented)
  child, and these records are never mutated in place, only appended.")

(defprotocol Store
  (child [st child-id])
  (activities-of [st child-id])
  (incident-reports-of [st child-id])
  (register-child! [st child])
  (record-activity! [st activity])
  (record-incident-report! [st incident-report]))

(defrecord MemStore [state]
  Store
  (child [_ child-id]
    (get-in @state [:children child-id]))
  (activities-of [_ child-id]
    (filter #(= child-id (:child-id %)) (:activities @state)))
  (incident-reports-of [_ child-id]
    (filter #(= child-id (:child-id %)) (:incident-reports @state)))
  (register-child! [_ child]
    (swap! state assoc-in [:children (:child-id child)] child))
  (record-activity! [_ activity]
    (swap! state update :activities (fnil conj []) activity))
  (record-incident-report! [_ incident-report]
    (swap! state update :incident-reports (fnil conj []) incident-report)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:children {} :activities [] :incident-reports []} seed)))))
