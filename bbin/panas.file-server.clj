#!/usr/bin/env bb

(require '[babashka.deps :as deps])
(deps/add-deps {:deps '{io.github.babashka/http-server {:git/sha "90c18ea14b716503b7e6b3179acfb2f0139998ab"}
                        io.github.keychera/panas.reload {:git/sha "23185ff18ea0d7fda44e6550b6f65b239e1e1dc0"}}})

(require '[babashka.http-server :refer [file-router serve]]
         '[babashka.fs :as fs]
         '[babashka.cli :as cli]
         'panas.reload)

(let [{:keys [help dir port]
       :or {dir "." port 42042}} (cli/parse-opts *command-line-args*)]
  (if help
    (println (:doc (meta #'serve)))
    (panas.reload/-main (file-router (fs/path dir)) {:port port}
                        {:watch-dir dir})))