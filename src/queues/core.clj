(ns queues.core
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defn blank-db
  "Função auxiliar usada para retornar um mapa de dados em branco, contendo apenas chaves e vetores vazios."
  []
  {:agents        []
   :jobs          []
   :active_jobs   []
   :finished_jobs []})

(def db (atom (blank-db)))

(defn find-by-id
  "Dada uma coleção de entidades que possuam :id e um valor id,
  retorna o elemento que da coleção correspondente à este id."
  [coll id]
  (first (take 1 (filter #(= (get % :id) id) coll))))

(defn filter-out-by-id
  "Dada uma coleção de entidades que possuam :id e um valor id,
  retorna uma versão da coleção sem este elemento."
  [coll id]
  (filter #(not= (get % :id) id) coll))

(defn update-multiple
  "Implementação auxiliar de update que permite que múltiplas chaves sejam atualizadas em uma única chamada.
  Recebe como parâmetros input-map (mapa base), keys (coleção de chaves que serão atualizadas)
  e function (função que será aplicada ao valor contido em cada chave para gerar um novo valor)."
  [input-map keys function]
  (reduce (fn [output-map current-key]
            (update output-map current-key function))
          input-map
          keys))

(defn normalize-skill
  "Dada uma string skill, usa expressão regular para identificá-la e retornar a palavra-chave correspondente.
  Função necessária devido ao fato do JSON de entrada apresentar valores divergentes nestes para skills."
  [skill]
  (cond (re-find #"bills" skill) :bills
        (re-find #"rewards" skill) :rewards
        :else :others))

(defn normalize-map-value
  "Dada uma entrada chave-valor (k e v), realiza as operações necessárias para retornar o valor v normalizado."
  [k v]
  (cond (= k :agents) (update-multiple v [:primary_skillset :secondary_skillset] #(map normalize-skill %))
        (= k :jobs) (update v :type #(normalize-skill %))
        :else v))

(defn normalize-map-key
  "Dada uma palavra-chave k, que tem sua origem no JSON de entrada,
  retorna a palavra-chave de entidade correspondente, caso exista."
  [k]
  (cond (= k :new_agent) :agents
        (= k :new_job) :jobs
        (= k :job_request) :requests
        :else k))

(defn json-to-array
  "Dada uma string JSON, retorna um vetor correspondente.
  Neste processo, as chaves do JSON são tratadas e transformadas em palavras-chave."
  [json]
  (if (string/blank? json)
    []
    (json/read-str json
                   :key-fn (fn [k] (normalize-map-key (keyword (string/trim k))))
                   :value-fn (fn [k v] (normalize-map-value k v)))))

(defn array-to-json
  "Dado um vetor, converte seu conteúdo para uma string JSON formatada com pretty-print."
  [array]
  (with-out-str (json/pprint array)))

(defn array-to-db
  "Dado um vetor, gera um mapa que agrupa seus elementos em vetores identificados por palavras-chave.
  Nestes vetores, os elementos estão ordenados de acordo com a ordem que aparecem no vetor original,
  ou seja, os elementos mais antigos aparecem no começo de cada lista e, os mais recentes, no final."
  [array]
  (reduce (fn [db current-item]
            (let [entry (first current-item)]
              (update db (key entry) #(conj (vec %) (val entry)))))
          {}
          array))

(defn filter-jobs-by-agent
  "Dados uma coleção de jobs e um agent,
  retorna os jobs que estejam associados com o agent informado."
  [jobs agent]
  (filter #(= (get % :agent_id) (get agent :id)) jobs))

(defn filter-jobs-by-skillset
  "Dadas duas coleções, contendo jobs e skills,
  retorna os jobs cujos tipos estejam contidos no conjunto de skills informado."
  [jobs skillset]
  (filter #(some #{(get % :type)} skillset) jobs))

(defn find-fittest-job-by-skillsets
  "Dados um vetor de jobs e um agent, tenta encontrar o job mais apropriado a ser realizado, caso exista.
  Retorna o primeiro job que possa ser realizado pelas skills primárias ou secundárias, nesta ordem."
  [jobs agent]
  (let [primary (get agent :primary_skillset)
        secondary (get agent :secondary_skillset)
        job (first (take 1 (mapcat #(filter-jobs-by-skillset jobs %) [primary secondary])))]
    (if (some? job) (assoc job :agent_id (get agent :id)))))

(defn sort-jobs-by-urgency
  "Dado um vetor de jobs, reorganiza seus elementos de modo que os jobs urgentes apareçam primeiro.
  Os jobs são ordenados em dois grupos e, dentro de cada grupo, respeitam a ordem em que apareciam no vetor original."
  [jobs]
  (let [jobs-by-urgency (group-by #(true? (get % :urgent)) jobs)]
    (vec (concat (get jobs-by-urgency true) (get jobs-by-urgency false)))))

(defn count-jobs-by-type
  "Dada uma coleção de jobs, retorna um mapa que informa a frequência de cada tipo de job."
  [jobs]
  (reduce (fn [output job]
            (let [type (get job :type)]
              (if (nil? (get output type))
                (assoc output type 1)
                (update output type inc))))
          {}
          jobs))

(defn entity-view
  "Dada uma chave k, retorna um JSON formatado contendo as entidades nela contidas."
  [k]
  (array-to-json (get @db k)))

(defn entity-add!
  "Dada uma chave k e um JSON, parseia o JSON em um elemento com id e o adiciona à coleção referenciada pela chave.
  Caso o novo elemento não possua id ou possua um id igual à algum elemento já existente, o mesmo será ignorado.
  Retorna um JSON formatado contendo o estado atual da coleção após a adição do elemento."
  [k json]
  (let [entity (normalize-map-value k (json-to-array json))
        id (get entity :id)]
    (if (and (some? id) (nil? (some #(= (get % :id) id) (get @db k))))
      (swap! db update-in [k] conj entity)))
  (entity-view k))

(defn agent-state
  "Dado um JSON que contém o id de um agent, retorna um JSON formatado
  contendo quantos jobs este agent já concluiu, categorizados por tipo."
  [json]
  (let [agent (json-to-array json)
        jobs (filter-jobs-by-agent (get @db :finished_jobs) agent)]
    (array-to-json (count-jobs-by-type jobs))))

(defn queue-state
  "Retorna um JSON formatado contendo informações sobre os jobs, categorizados entre
  ítens concluídos, ítens que estão sendo realizados e ítens que aguardam na fila."
  []
  (array-to-json {"finished" (get @db :finished_jobs)
                  "active"   (get @db :active_jobs)
                  "waiting"  (get @db :jobs)}))

(defn move-job!
  "Dado um job e chaves que identificam as coleções de origem e de destino,
  remove o job da coleção de origem e o adiciona na coleção de destino."
  [job origin-k destiny-k]
  (when (some? job)
    (swap! db assoc-in [origin-k] (filter-out-by-id (get @db origin-k) (get job :id)))
    (swap! db update-in [destiny-k] conj job)))

(defn dequeue!
  "Dado um id de agent, retorna o id do job mais adequado a ser realizado pelo mesmo, caso exista, ou nil.
  Caso seja encontrado um fittest-job a ser realizado, o mesmo é movido da coleção de jobs para active_jobs.
  Caso o agent possua um current-job, o mesmo é considerado feito e movido de active_jobs para finished_jobs."
  [agent-id]
  (let [agent (find-by-id (get @db :agents) agent-id)
        fittest-job (find-fittest-job-by-skillsets (sort-jobs-by-urgency (get @db :jobs)) agent)
        current-job (first (filter-jobs-by-agent (get @db :active_jobs) agent))]
    (move-job! fittest-job :jobs :active_jobs)
    (move-job! current-job :active_jobs :finished_jobs)
    (get fittest-job :id)))

(defn job-request!
  "Dado um JSON de requisição de job contendo um agent_id, processa a requisição e tenta encontrar um job adequado.
  Retorna um JSON formatado contendo informações da atribuição de job realizada, caso exista."
  [json]
  (let [agent-id (get (json-to-array json) :agent_id)
        job-id (dequeue! agent-id)]
    (array-to-json
      (if (or (nil? agent-id) (nil? job-id))
           {}
           {"agent_id" agent-id "job_id" job-id}))))

(defn- debug
  "Retorna o estado atual do mapa de dados. (dev)"
  []
  (array-to-json @db))

(defn- setup!
  "Inicializa o mapa de dados de acordo com as entradas de new_agent, new_job e job_request existentes no JSON.
   Após inicializar o mapa, processa os requests encontrados e retorna o estado atual do mapa de dados. (dev)"
  [json]
  (reset! db (merge (blank-db) (array-to-db (json-to-array json))))
  (mapcat #(job-request! (array-to-json %)) (get @db :requests))
  (debug))

(defn- clear!
  "Inicializa o mapa de dados em branco. (dev)"
  []
  (reset! db (blank-db))
  (debug))

(defroutes queues-routes
           ;(POST "/setup" {body :body} (setup! (slurp body))) ;(dev)
           ;(GET "/clear" {} (clear!)) ;(dev)
           ;(GET "/debug" {} (debug)) ;(dev)

           (GET "/agents" {} (entity-view :agents))
           (POST "/agents/add" {body :body} (entity-add! :agents (slurp body)))
           (POST "/agents/state" {body :body} (agent-state (slurp body)))

           (GET "/jobs" {} (queue-state))
           (POST "/jobs/add" {body :body} (entity-add! :jobs (slurp body)))
           (POST "/jobs/request" {body :body} (job-request! (slurp body)))

           (route/resources "/")
           (route/not-found ""))

(def handler
  "Handler referenciado pelo plugin Lein-Ring."
  (handler/api queues-routes))
