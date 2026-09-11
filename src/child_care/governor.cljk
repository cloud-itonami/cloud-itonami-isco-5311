(ns child-care.governor
  "ChildCareGovernor — the independent safety/traceability layer for the
  ISCO-08 5311 independent child-care-worker actor. The Care Advisor
  proposes actions (activity, incident-report); it has no notion of
  child provenance, allergy risk, or incident-response requirements, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD — the itonami-actor pattern (independent Governor gates a
  proposing actor) applied to this occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. A `:meal` activity for a child
  with known allergies ALWAYS requires human sign-off, and ANY
  incident-report ALWAYS escalates to human sign-off — neither can ever
  be auto-approved/auto-cleared.

  HARD invariants for :child-care/propose:
    1. Child provenance     — an activity or incident-report must
       reference a registered (guardian-consented) child.
    2. No-actuation          — the proposal must not directly mutate an
       activity/incident-report record outside the record-activity!/
       record-incident-report! path (effect must be :propose, never a
       raw store write).
    3. Meal-allergy safety   — a `:meal` activity for a child with a
       non-empty `allergies` set always requires :high or higher
       safety-class, forcing human sign-off; it is never auto-approved
       regardless of confidence.
  SOFT:
    4. Every incident-report always escalates to human sign-off (no
       autonomous incident clearance).
    5. Confidence floor → escalate."
  (:require [child-care.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- meal-with-allergy-risk? [found-child proposal]
  (and (= :activity (:kind proposal))
       (= :meal (:category proposal))
       found-child
       (seq (:allergies found-child))))

(defn- hard-violations [{:keys [child-fn]} proposal]
  (let [{:keys [child-id safety-class effect]} proposal
        found-child (child-fn child-id)]
    (cond-> []
      (nil? found-child)
      (conj {:rule :no-child :detail (str "未登録 child " child-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and (meal-with-allergy-risk? found-child proposal)
           (< (safety-rank (or safety-class :none)) (safety-rank :high)))
      (conj {:rule :meal-allergy-safety
             :detail "allergies のある child への meal activity は :high 以上の safety-class が必須"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:child-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 0.0)
        incident? (= :incident-report (:kind proposal))]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      incident?
      {:decision :human-approval :violations [] :confidence confidence
       :reason :incident-report}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `child-care.store/Store` implementation."
  [store]
  {:child-fn #(store/child store %)})
