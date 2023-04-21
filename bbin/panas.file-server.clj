#!/usr/bin/env bb

(require '[babashka.deps :as deps])
(deps/add-deps {:deps '{io.github.babashka/http-server {:git/sha "8febc087c4098450e4bdecc135eece8015df03fc"}
                        io.github.keychera/panas.reload {:git/sha "d80ee6d6dbfa8bab2a52e87f1da0e1682cef167c"}}})

(require '[babashka.http-server :refer [file-router]]
         '[babashka.fs :as fs]
         '[babashka.cli :as cli]
         'panas.reload)

(let [{:keys [help dir url port]
       :or {dir "." url "0.0.0.0" port 42042}} (cli/parse-opts *command-line-args*)]
  (if help
    (println "Serves static assets using web server.
Options:
  * `:dir` - directory from which to serve assets
  * `:url` - url, default to 0.0.0.0
  * `:port` - port, default to 42042")
    (panas.reload/-main (file-router (fs/path dir)) {:url url :port port}
                        {:watch-dir dir})))