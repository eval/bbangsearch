# Changelog

## v0.7.0 (2023-11-21)

- Support `bangs.edn` in project-folders  
  Full config-chain becomes:  
  (parent-)folder bangs.edn > user-config > built-in
- Support `:aliases`  
  E.g. to have the right version when doing `bbang java some query`, add the following map-entry to (a project-folder) `bangs.edn`:  
  `"java19" {:aliases ["java"]}`  
  Aliases of aliases work as you would expect: `"java" {:aliases ["j"]}` points `j` to `java19`.
- Show aliases in bang listing `bangs:ls`
- Support install via homebrew
- Add standalone binaries for Mac&Linux
- Added/fixed bangs: `cljd`, `drdk`, `drtv`, `ghdbf` (old-skool GitHub feed), `ghrel` (like `ghrepo`), `java19`, `java20`, `java21` (aliased as `java`), `pgdoc14`, `pgdoc15`, `pgdoc16`, `pgdoc`, `rails61` (aliased as `rails6`), `rails70`, `rails71` (aliased as `rails7`)
- Add jump-to functionality to `gem` and `rdoc` (search for `@some-gem`)  
