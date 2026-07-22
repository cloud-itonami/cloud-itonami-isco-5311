(ns child-care.store-contract-test
  "MemStore ≡ DatomicStore parity for the Store protocol — proves the
  backend swap (ADR-2607011000 injection boundary) is real: the same
  sequence of operations against either backend produces the same
  observable results."
  (:require [clojure.test :refer [deftest is testing]]
            [child-care.store :as store]
            [child-care.governor :as governor]))

(defn- exercise [s]
  (store/register-child! s {:child-id "child-1" :guardian-consent? true :allergies #{"peanuts"}})
  (store/record-activity! s {:activity-id "a1" :child-id "child-1" :category :play})
  (store/record-incident-report! s {:report-id "r1" :child-id "child-1" :severity :low})
  {:child (store/child s "child-1")
   :activities (store/activities-of s "child-1")
   :incident-reports (store/incident-reports-of s "child-1")})

(deftest mem-and-datomic-parity
  (testing "same operations against MemStore and DatomicStore observe the same results"
    (let [mem (exercise (store/mem-store))
          dat (exercise (store/datomic-store))]
      (is (true? (:guardian-consent? (:child mem))))
      (is (true? (:guardian-consent? (:child dat))))
      (is (= #{"peanuts"} (:allergies (:child mem))))
      (is (= #{"peanuts"} (:allergies (:child dat))))
      (is (= 1 (count (:activities mem))))
      (is (= 1 (count (:activities dat))))
      (is (= :play (:category (first (:activities mem)))))
      (is (= :play (:category (first (:activities dat)))))
      (is (= 1 (count (:incident-reports mem))))
      (is (= 1 (count (:incident-reports dat))))
      (is (= :low (:severity (first (:incident-reports mem)))))
      (is (= :low (:severity (first (:incident-reports dat))))))))

(deftest datomic-store-nil-lookup-and-empty-filters
  (testing "unregistered child lookup is nil, activities/incident-reports on an unknown child are empty"
    (let [dat (store/datomic-store)]
      (is (nil? (store/child dat "no-such")))
      (is (empty? (store/activities-of dat "no-such")))
      (is (empty? (store/incident-reports-of dat "no-such"))))))

(deftest governor-env-for-store-works-against-datomic-store
  (testing "child-care.governor/env-for-store's :child-fn resolves correctly against DatomicStore too"
    (let [dat (store/datomic-store)]
      (store/register-child! dat {:child-id "child-2" :guardian-consent? true :allergies #{}})
      (let [env (governor/env-for-store dat)]
        (is (some? ((:child-fn env) "child-2")))
        (is (nil? ((:child-fn env) "no-such")))))))
