(ns queues.core-test
  (:require [queues.core :as core]
            [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clojure.string :as string]))

(background (after :facts (reset! core/db (core/blank-db))))

(defn clean-json
  "Função auxiliar usada para limpar a string JSON, removendo caracteres adicionais como
  espaços e quebras de linha, com o objetivo de facilitar as asserções nos testes."
  [json]
  (string/replace json #"[\n\r\s]" ""))

(facts "find-by-id"
       (fact
         (let [coll [{:id 100}
                     {:id 200}
                     {:id 300}]]
           (core/find-by-id coll 100) => {:id 100}
           (core/find-by-id coll 201) => nil
           (core/find-by-id coll 300) => {:id 300})))

(facts "filter-out-by-id"
       (fact
         (let [coll [{:id 100}
                     {:id 200}
                     {:id 300}]]
           (core/filter-out-by-id coll 100) => [{:id 200} {:id 300}]
           (core/filter-out-by-id coll 201) => [{:id 100} {:id 200} {:id 300}]
           (core/filter-out-by-id coll 300) => [{:id 100} {:id 200}])))

(facts "update-multiple"
       (fact
         (let [data {:id         100
                     :first-name "Patricia"
                     :last-name  "Smith"
                     :user-name  "pattismith46"}]

           (core/update-multiple data [:first-name :last-name] #(string/upper-case %)) =>
           {:id         100
            :first-name "PATRICIA"
            :last-name  "SMITH"
            :user-name  "pattismith46"})))

(facts "normalize-skill"
       (fact
         (core/normalize-skill "bills-question") => :bills
         (core/normalize-skill "rewards_questions") => :rewards
         (core/normalize-skill "existential-question") => :others))

(facts "normalize-map-value"
       (fact
         (let [agent-entry {:id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"
                            :name               "John Smith"
                            :primary_skillset   ["bills-questions" "operational_question"]
                            :secondary_skillset []}
               job-entry {:id "f26e890b-3fae-4804-bfd9-7762aa0bac36" :type "rewards_question" :urgent true}
               request-entry {:agent_id "f26e890b-3fae-4804-bfd9-7762aa0bac36"}]
           (let [v (core/normalize-map-value :agents agent-entry)]
             (get v :primary_skillset) => [:bills :others]
             (get v :secondary_skillset) => [])
           (let [v (core/normalize-map-value :jobs job-entry)]
             (get v :type) => :rewards)
           (let [v (core/normalize-map-value :requests request-entry)]
             v => request-entry))))

(facts "normalize-map-key"
       (fact
         (core/normalize-map-key :new_agent) => :agents
         (core/normalize-map-key :new_job) => :jobs
         (core/normalize-map-key :job_request) => :requests
         (core/normalize-map-key :id) => :id))

(facts "json-to-array"
       (fact
         (let [json "[\n  {\n    \"new_agent\": {\n      \"id\": \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\",\n      \"name\": \"BoJack Horseman\",\n      \"primary_skillset\": [\"bills-questions\"],\n      \"secondary_skillset\": []\n    }\n  },\n  {\n    \"new_job\": {\n      \"id\": \"f26e890b-df8e-422e-a39c-7762aa0bac36\",\n      \"type\": \"rewards_question\",\n      \"urgent\": false\n    }\n  },\n  {\n    \"new_agent\": {\n      \"id\": \"ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88\",\n      \"name\": \"Mr. Peanut Butter\",\n      \"primary_skillset\": [\"rewards_question\"],\n      \"secondary_skillset\": [\"bills_questions\"]\n    }\n  },\n  {\n    \"new_job\": {\n      \"id\": \"690de6bc-163c-4345-bf6f-25dd0c58e864\",\n      \"type\": \"bills_question\",\n      \"urgent\": false\n    }\n  },\n  {\n    \"new_job\": {\n      \"id\": \"c0033410-981c-428a-954a-35dec05ef1d2\",\n      \"type\": \"bills_questions\",\n      \"urgent\": true\n    }\n  },\n  {\n    \"job_request\": {\n      \"agent_id \": \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\"\n    }\n  },\n  {\n    \"job_request\": {\n      \"agent_id\": \"ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88\"\n    }\n  }\n]\n"]
           (core/json-to-array nil) => []
           (core/json-to-array " ") => []
           (core/json-to-array json) =>
           [{:agents {:id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                      :name               "BoJack Horseman",
                      :primary_skillset   [:bills],
                      :secondary_skillset []}}
            {:jobs {:id "f26e890b-df8e-422e-a39c-7762aa0bac36", :type :rewards, :urgent false}}
            {:agents {:id                 "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88",
                      :name               "Mr. Peanut Butter",
                      :primary_skillset   [:rewards],
                      :secondary_skillset [:bills]}}
            {:jobs {:id "690de6bc-163c-4345-bf6f-25dd0c58e864", :type :bills, :urgent false}}
            {:jobs {:id "c0033410-981c-428a-954a-35dec05ef1d2", :type :bills, :urgent true}}
            {:requests {:agent_id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
            {:requests {:agent_id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}}])))

(facts "array-to-json"
       (fact
         (let [array [{"job_assigned" {"job_id"   "c0033410-981c-428a-954a-35dec05ef1d2",
                                       "agent_id" "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
                      {"job_assigned" {"job_id"   "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                       "agent_id" "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}}]]
           (clean-json (core/array-to-json array)) =>
           "[{\"job_assigned\":{\"job_id\":\"c0033410-981c-428a-954a-35dec05ef1d2\",\"agent_id\":\"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\"}},{\"job_assigned\":{\"job_id\":\"f26e890b-df8e-422e-a39c-7762aa0bac36\",\"agent_id\":\"ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88\"}}]")))

(facts "array-to-db"
       (fact
         (let [array [{:agents {:id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"
                                :name               "BoJack Horseman"
                                :primary_skillset   [:bills]
                                :secondary_skillset []}}
                      {:jobs {:id "f26e890b-df8e-422e-a39c-7762aa0bac36" :type :rewards :urgent false}}
                      {:agents {:id                 "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88",
                                :name               "Mr. Peanut Butter",
                                :primary_skillset   [:rewards],
                                :secondary_skillset [:bills]}}
                      {:jobs {:id "690de6bc-163c-4345-bf6f-25dd0c58e864", :type :bills, :urgent false}}
                      {:jobs {:id "c0033410-981c-428a-954a-35dec05ef1d2", :type :bills, :urgent true}}
                      {:requests {:agent_id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
                      {:requests {:agent_id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}}]]

           (core/array-to-db array) =>
           {:agents   [{:id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"
                        :name               "BoJack Horseman"
                        :primary_skillset   [:bills]
                        :secondary_skillset []}
                       {:id                 "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"
                        :name               "Mr. Peanut Butter"
                        :primary_skillset   [:rewards]
                        :secondary_skillset [:bills]}],
            :jobs     [{:id "f26e890b-df8e-422e-a39c-7762aa0bac36", :type :rewards, :urgent false}
                       {:id "690de6bc-163c-4345-bf6f-25dd0c58e864", :type :bills, :urgent false}
                       {:id "c0033410-981c-428a-954a-35dec05ef1d2", :type :bills, :urgent true}],
            :requests [{:agent_id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}
                       {:agent_id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}]})))

(facts "filter-jobs-by-agent"
       (fact
         (let [jobs [{:id 1 :agent_id 100}
                     {:id 2 :agent_id 100}
                     {:id 3 :agent_id 200}
                     {:id 4 :agent_id 300}
                     {:id 5}]
               agent-100 {:id 100 :name "Abby"}
               agent-200 {:id 200 :name "Beth"}
               agent-300 {:id 300 :name "Carl"}]

           (count (core/filter-jobs-by-agent jobs agent-100)) => 2
           (count (core/filter-jobs-by-agent jobs agent-200)) => 1
           (count (core/filter-jobs-by-agent jobs agent-300)) => 1
           (count (core/filter-jobs-by-agent jobs {:name "Zedd"})) => 1
           (count (core/filter-jobs-by-agent jobs {})) => 1
           (count (core/filter-jobs-by-agent [] [])) => 0)))

(facts "filter-jobs-by-skillset"
       (fact
         (let [jobs [{:id 1 :type :bills}
                     {:id 2 :type :rewards}
                     {:id 3 :type :rewards}
                     {:id 4 :type :bills}
                     {:id 5 :type :bills}]]

           (count (core/filter-jobs-by-skillset jobs [:bills :rewards])) => 5
           (count (core/filter-jobs-by-skillset jobs [:bills])) => 3
           (count (core/filter-jobs-by-skillset jobs [:rewards])) => 2
           (count (core/filter-jobs-by-skillset jobs [:others])) => 0
           (count (core/filter-jobs-by-skillset jobs [])) => 0
           (count (core/filter-jobs-by-skillset [] [:bills])) => 0
           (count (core/filter-jobs-by-skillset [] [])) => 0)))

(facts "find-fittest-job-by-skillsets"
       (fact
         (let [jobs [{:id 1 :type :bills}
                     {:id 2 :type :rewards}
                     {:id 3 :type :cancellation}
                     {:id 4 :type :orders}
                     {:id 5 :type :savings}]
               agent-1 {:id                 1
                        :primary_skillset   [:bills]
                        :secondary_skillset [:rewards]}
               agent-2 {:id                 2
                        :primary_skillset   [:savings]
                        :secondary_skillset [:orders]}
               agent-3 {:id                 3
                        :primary_skillset   [:singing]
                        :secondary_skillset [:cancellation]}
               agent-4 {:id                 4
                        :primary_skillset   []
                        :secondary_skillset [:rewards]}
               agent-5 {:id                 5
                        :primary_skillset   [:dancing]
                        :secondary_skillset [:singing]}
               agent-6 {:id                 6
                        :primary_skillset   []
                        :secondary_skillset []}
               agent-7 {:id                 7
                        :primary_skillset   [:swimming :flying]
                        :secondary_skillset [:climbing :orders]}]

           (get (core/find-fittest-job-by-skillsets jobs agent-1) :type) => :bills
           (get (core/find-fittest-job-by-skillsets jobs agent-2) :type) => :savings
           (get (core/find-fittest-job-by-skillsets jobs agent-3) :type) => :cancellation
           (get (core/find-fittest-job-by-skillsets jobs agent-4) :type) => :rewards
           (get (core/find-fittest-job-by-skillsets jobs agent-5) :type) => nil
           (get (core/find-fittest-job-by-skillsets jobs agent-6) :type) => nil
           (get (core/find-fittest-job-by-skillsets jobs agent-7) :type) => :orders)))

(facts "sort-jobs-by-urgency"
       (fact
         (let [jobs-1 [{:id 1 :urgent false}
                       {:id 2 :urgent true}
                       {:id 3 :urgent false}
                       {:id 4 :urgent true}
                       {:id 5 :urgent false}]
               jobs-2 [{:id 1 :urgent false}
                       {:id 2 :urgent false}]
               jobs-3 [{:id 1 :urgent true}]
               jobs-4 [{:id 1 :urgent true}
                       {:id 2 :urgent true}
                       {:id 3 :urgent false}]]

           (map #(get % :id) (core/sort-jobs-by-urgency jobs-1)) => [2 4 1 3 5]
           (map #(get % :id) (core/sort-jobs-by-urgency jobs-2)) => [1 2]
           (map #(get % :id) (core/sort-jobs-by-urgency jobs-3)) => [1]
           (map #(get % :id) (core/sort-jobs-by-urgency jobs-4)) => [1 2 3]
           (map #(get % :id) (core/sort-jobs-by-urgency [])) => [])))

(facts "count-jobs-by-type"
       (test
         (let [jobs-1 [{:id 1 :type :bills}
                       {:id 2 :type :rewards}
                       {:id 3 :type :bills}
                       {:id 4 :type :cards}
                       {:id 5 :type :bills}
                       {:id 6 :type :rewards}]
               jobs-2 [{:id 1 :type :bills}]]

           (core/count-jobs-by-type jobs-1) => {:bills 3 :rewards 2 :cards 1}
           (core/count-jobs-by-type jobs-2) => {:bills 1}
           (core/count-jobs-by-type []) => {})))

(facts "entity-view"
       (fact
         (reset! core/db {:agents   [{:id "101"} {:id "102"} {:id "103"} {:id "104"} {:id "105"}]
                          :jobs     [{:id "201"} {:id "202"} {:id "203"}]
                          :requests []})
         (clean-json (core/entity-view :agents)) => "[{\"id\":\"101\"},{\"id\":\"102\"},{\"id\":\"103\"},{\"id\":\"104\"},{\"id\":\"105\"}]"
         (clean-json (core/entity-view :jobs)) => "[{\"id\":\"201\"},{\"id\":\"202\"},{\"id\":\"203\"}]"
         (clean-json (core/entity-view :requests)) => "[]"
         (clean-json (core/entity-view :foo)) => "null"))

(facts "entity-add!"
       (fact
         (core/entity-add! :agents "{\"id\":\"100\",\"name\":\"Sunny\",\"primary_skillset\":[\"bills-questions\"],\"secondary_skillset\":[]}")
         (core/entity-add! :agents "{\"id\":\"101\",\"name\":\"Moony\",\"primary_skillset\":[\"rewards_question\"],\"secondary_skillset\":[\"bills_questions\"]}")
         (core/entity-add! :jobs "{\"id\":\"200\",\"type\":\"bills_question\",\"urgent\":false}")
         (core/entity-add! :jobs "{\"id\":\"200\",\"type\":\"bills_question\",\"urgent\":false}")
         (core/entity-add! :jobs "{\"id\":\"200\",\"type\":\"bills-question\",\"urgent\":true}")
         (core/entity-add! :finished_jobs "{\"id\":\"300\",\"type\":\"bills_question\",\"urgent\":false}")
         (core/entity-add! :finished_jobs "{\"code\":\"301\",\"type\":\"rewards_question\",\"urgent\":true}")

         (count (get (deref core/db) :agents)) => 2
         (count (get (deref core/db) :jobs)) => 1
         (count (get (deref core/db) :finished_jobs)) => 1
         (count (get (deref core/db) :foo)) => 0))

(facts "agent-state"
       (fact
         (reset! core/db {:finished_jobs [{:id "201" :type :bills :agent_id "101"}
                                          {:id "202" :type :rewards :agent_id "101"}
                                          {:id "203" :type :savings :agent_id "102"}
                                          {:id "204" :type :orders :agent_id "102"}
                                          {:id "205" :type :promos :agent_id "103"}
                                          {:id "206" :type :cards :agent_id "103"}
                                          {:id "207" :type :bills :agent_id "101"}
                                          {:id "208" :type :orders :agent_id "102"}
                                          {:id "209" :type :promos :agent_id "103"}
                                          {:id "210" :type :bills :agent_id "101"}]})

         (clean-json (core/agent-state "{\"id\":\"101\"}")) => "{\"bills\":3,\"rewards\":1}"
         (clean-json (core/agent-state "{\"id\":\"102\"}")) => "{\"savings\":1,\"orders\":2}"
         (clean-json (core/agent-state "{\"id\":\"103\"}")) => "{\"promos\":2,\"cards\":1}"
         (clean-json (core/agent-state "{\"id\":\"104\"}")) => "{}"))

(facts "queue-state"
       (fact
         (reset! core/db {:finished_jobs []
                          :active_jobs   [{:id "2"} {:id "4"}]
                          :jobs          [{:id "1"} {:id "3"} {:id "5"}]})

         (clean-json (core/queue-state)) =>
         "{\"finished\":[],\"active\":[{\"id\":\"2\"},{\"id\":\"4\"}],\"waiting\":[{\"id\":\"1\"},{\"id\":\"3\"},{\"id\":\"5\"}]}"))

(facts "move-job!"
       (fact
         (let [job {:id "100"}]
           (reset! core/db {:jobs          [job]
                            :active_jobs   []
                            :finished_jobs [{:id "200"} {:id "300"}]})
           (count (get (deref core/db) :jobs)) => 1
           (count (get (deref core/db) :active_jobs)) => 0
           (count (get (deref core/db) :finished_jobs)) => 2

           (core/move-job! job :jobs :active_jobs)
           (count (get (deref core/db) :jobs)) => 0
           (count (get (deref core/db) :active_jobs)) => 1
           (count (get (deref core/db) :finished_jobs)) => 2

           (core/move-job! job :active_jobs :finished_jobs)
           (count (get (deref core/db) :jobs)) => 0
           (count (get (deref core/db) :active_jobs)) => 0
           (count (get (deref core/db) :finished_jobs)) => 3

           (core/move-job! job :finished_jobs :jobs)
           (count (get (deref core/db) :jobs)) => 1
           (count (get (deref core/db) :active_jobs)) => 0
           (count (get (deref core/db) :finished_jobs)) => 2)))

(facts "dequeue!"
       (fact
         (let [test-db {:agents        [{:id                 "101"
                                         :primary_skillset   [:bills]
                                         :secondary_skillset [:rewards]}
                                        {:id                 "102"
                                         :primary_skillset   [:rewards]
                                         :secondary_skillset [:bills]}
                                        {:id                 "103"
                                         :primary_skillset   [:bills]
                                         :secondary_skillset [:savings]}
                                        {:id                 "104"
                                         :primary_skillset   [:savings]
                                         :secondary_skillset [:promos]}
                                        {:id                 "105"
                                         :primary_skillset   [:promos]
                                         :secondary_skillset [:cards]}],
                        :jobs          [{:id "201", :type :bills, :urgent false}
                                        {:id "202", :type :rewards, :urgent false}
                                        {:id "203", :type :bills, :urgent true}
                                        {:id "204", :type :savings, :urgent false}
                                        {:id "205", :type :rewards, :urgent true}
                                        {:id "206", :type :cards, :urgent true}
                                        {:id "207", :type :savings, :urgent true}
                                        {:id "208", :type :promos, :urgent false}
                                        {:id "209", :type :bills, :urgent false}
                                        {:id "210", :type :promos, :urgent true}]
                        :finished_jobs []
                        :active_jobs   []}
               requests-1 ["101" "101" "102" "102" "103" "103" "104" "104" "105" "105" "101" "102" "103" "104" "105"]
               requests-2 ["105" "104" "103" "102" "101" "101" "102" "103" "104" "105"]
               requests-3 ["101" "103" "101" "103" "101" "103" "102" "103" "102" "104"]
               requests-4 ["101" "101" "101" "101" "101" "101"]
               requests-5 ["105" "105" "105"]]

           (reset! core/db test-db)
           (map core/dequeue! requests-1) => ["203" "201" "205" "202" "209" "207" "204" "210" "208" "206" nil nil nil nil nil]
           (count (get (deref core/db) :jobs)) => 0
           (count (get (deref core/db) :active_jobs)) => 0
           (count (get (deref core/db) :finished_jobs)) => 10

           (reset! core/db test-db)
           (map core/dequeue! requests-2) => ["210" "207" "203" "205" "201" "209" "202" "204" "208" "206"]
           (count (get (deref core/db) :jobs)) => 0
           (count (get (deref core/db) :active_jobs)) => 5
           (count (get (deref core/db) :finished_jobs)) => 5

           (reset! core/db test-db)
           (map core/dequeue! requests-3) => ["203" "201" "209" "207" "205" "204" "202" nil nil "210"]
           (count (get (deref core/db) :jobs)) => 2
           (count (get (deref core/db) :active_jobs)) => 2
           (count (get (deref core/db) :finished_jobs)) => 6

           (reset! core/db test-db)
           (map core/dequeue! requests-4) => ["203" "201" "209" "205" "202" nil]
           (count (get (deref core/db) :jobs)) => 5
           (count (get (deref core/db) :active_jobs)) => 0
           (count (get (deref core/db) :finished_jobs)) => 5

           (reset! core/db test-db)
           (map core/dequeue! requests-5) => ["210" "208" "206"]
           (count (get (deref core/db) :jobs)) => 7
           (count (get (deref core/db) :active_jobs)) => 1
           (count (get (deref core/db) :finished_jobs)) => 2

           (reset! core/db test-db)
           (map core/dequeue! []) => []
           (count (get (deref core/db) :jobs)) => 10
           (count (get (deref core/db) :active_jobs)) => 0
           (count (get (deref core/db) :finished_jobs)) => 0)))

(facts "job-request!"
       (fact
         (reset! core/db {:agents        [{:id                 "101"
                                           :primary_skillset   [:bills]
                                           :secondary_skillset [:rewards]}
                                          {:id                 "102"
                                           :primary_skillset   [:rewards]
                                           :secondary_skillset [:bills]}
                                          {:id                 "103"
                                           :primary_skillset   [:bills]
                                           :secondary_skillset [:savings]}
                                          {:id                 "104"
                                           :primary_skillset   [:savings]
                                           :secondary_skillset [:promos]}
                                          {:id                 "105"
                                           :primary_skillset   [:promos]
                                           :secondary_skillset [:cards]}],
                          :jobs          [{:id "201", :type :bills, :urgent false}
                                          {:id "202", :type :rewards, :urgent false}
                                          {:id "203", :type :bills, :urgent true}
                                          {:id "204", :type :savings, :urgent false}
                                          {:id "205", :type :rewards, :urgent true}
                                          {:id "206", :type :cards, :urgent true}
                                          {:id "207", :type :savings, :urgent true}
                                          {:id "208", :type :promos, :urgent false}
                                          {:id "209", :type :bills, :urgent false}
                                          {:id "210", :type :promos, :urgent true}]
                          :finished_jobs []
                          :active_jobs   []})

         (clean-json (core/job-request! "{\"agent_id\":\"101\"}")) => "{\"agent_id\":\"101\",\"job_id\":\"203\"}"
         (clean-json (core/job-request! "{\"agent_id\":\"102\"}")) => "{\"agent_id\":\"102\",\"job_id\":\"205\"}"
         (clean-json (core/job-request! "{\"agent_id\":\"103\"}")) => "{\"agent_id\":\"103\",\"job_id\":\"201\"}"
         (clean-json (core/job-request! "{\"agent_id\":\"104\"}")) => "{\"agent_id\":\"104\",\"job_id\":\"207\"}"
         (clean-json (core/job-request! "{\"agent_id\":\"105\"}")) => "{\"agent_id\":\"105\",\"job_id\":\"210\"}"
         (clean-json (core/job-request! "{\"agent_id\":\"105\"}")) => "{\"agent_id\":\"105\",\"job_id\":\"208\"}"
         (clean-json (core/job-request! "{\"agent_id\":\"105\"}")) => "{\"agent_id\":\"105\",\"job_id\":\"206\"}"
         (clean-json (core/job-request! "{\"agent_id\":\"105\"}")) => "{}"
         (clean-json (core/job-request! "{\"agent_id\":\"200\"}")) => "{}"
         (clean-json (core/job-request! "{\"id\":\"101\"}")) => "{}"
         (clean-json (core/job-request! "{}")) => "{}"))
