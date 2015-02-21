(set-env!
 :source-paths    #{"src/cljs" "src/clj"}
 :resource-paths  #{"resources"}
 :dependencies '[[org.clojure/clojurescript "0.0-2814"]
                 [adzerk/boot-cljs      "0.0-2814-1" :scope "test"]
                 [adzerk/boot-reload    "0.2.4"      :scope "test"]
                 [pandeiro/boot-http    "0.6.1"      :scope "test"]
                 [jeluard/boot-notify   "0.1.1"      :scope "test"]
                 [boot-garden           "1.2.5-1"    :scope "test"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [matchbox "0.0.5-SNAPSHOT" :exclusions [com.firebase/firebase-client-jvm]]
                 [rum "0.2.4" :exclusions [com.cemerick/austin]]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[boot-garden.core      :refer [garden]]
 '[jeluard.boot-notify   :refer [notify]])

(deftask build []
  (comp (notify)
        (cljs)
        (garden :styles-var 'chaf-light.styles/screen
                :vendors ["webkit"]
                :auto-prefix #{:align-items}
                :output-to "css/garden.css")))

(deftask dev-run []
  (comp (serve)
        (watch)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced
                       :compiler-options {:closure-defines {:goog.DEBUG false}}}
                      garden {:pretty-print false})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :unified-mode true
                       :source-map true}
                 reload {:on-jsload 'chaf-light.app/init})
  identity)

(deftask prod
  "Simple alias to run application in production mode
   No REPL or automatic reloading code inserted."
  []
  (comp (production)
        (serve)
        (watch)
        (build)))

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (dev-run)))
