#!/usr/bin/env bb

(require '[babashka.deps :as deps])
(deps/add-deps {:deps '{io.github.babashka/http-server {:git/sha "90c18ea14b716503b7e6b3179acfb2f0139998ab"}
                        io.github.keychera/panas.reload {:git/sha "c875d4f019ab15052b034282a8a02c298c5ade10"}}})

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