# bbangsearch❗

A CLI for [DuckDuckGo's bang❗ searches](https://duckduckgo.com/bangs) written in [Babashka](https://babashka.org/).

## Rationale

I think bang searches are really great. Having DDG as my default search-engine, it's often easier to just type `!gh project` in the address bar than to cycle through open browser tabs to find the page of said GitHub project.

I found there's also some shortcomings:
- I miss a 'jump-to'-mode  
  often there's no need to search, just to visit the thing you know is there.
- some bangs are no longer working
- no option to add custom bangs
- they only work in the browser

Enter `bbang`:
* open (or get the url of) any of the ~14k bang search pages via the commandline
* allow overriding/adding bangs
* list all bangs (in an easily grep-able format)
* allow for 'jump-to' functionality

### examples

```shell
# I. Use any of the regular DDG bangs (opens default browser),
# e.g. gh (github), yt (youtube), dd (devdocs.io) etc.
$ bbang gh some project
$ bbang yt topic "and exact phrase"
$ bbang dd git rebase

# ...get the url
$ bbang dd git rebase --url
# => https://devdocs.io/#q=git+rebase


# II. Additional built-in bangs
# ghrepo: visit the GitHub project of the repository you're working on
$ bbang ghrepo

# ...search it
$ bbang ghrepo _ some query

# ...visit/search any one of your repositories
$ bbang ghrepo deps-try
$ bbang ghrepo deps-try some query

# III. Add custom bangs
# search all tickets
$ bbang proj/tickets some query

# visit specific ticket
$ bbang proj/tickets PROJ-123
```

## Installation

### Homebrew (Linux and macOS)

#### Install

``` bash
$ brew install eval/brew/bbang
# For future upgrades do:
$ brew update && brew upgrade bbang
```

There's also the unstable releases (tracking the mainline):
``` bash
$ brew install --head eval/brew/bbang
# For future upgrades do:
$ brew update && brew reinstall bbang
```

### bbin

[bbin](https://github.com/babashka/bbin) allows for easy installation of Babashka scripts.

#### Prerequisites

[install bbin](https://github.com/babashka/bbin#installation) (make sure to adjust $PATH).

```shell
# latest version from mainline
$ bbin install io.github.eval/bbangsearch
$ bbang -h

# as something else
$ bbin install io.github.eval/bbangsearch --as b
```

### manual (Windows, Linux and macOS)

#### Prerequisites

* Install [babashka](https://github.com/babashka/babashka#installation)

Verify that the following commands work:

``` bash
$ bb --version
babashka v1.3.186
```

#### Installation

* Download [the latest stable bb-jar](https://github.com/eval/bbangsearch/releases/tag/stable).
* Put an executable wrapper-script on $PATH. For example (for Linux and macOS):
```bash
#!/usr/bin/env sh

exec bb /absolute/path/to/bbang-bb.jar "$@"
```

### standalone

Download, chmod +x and put somewhere on PATH:
``` shell
# Mac aarch64
$ curl -sL https://github.com/eval/bbangsearch/releases/download/stable/bbang-mac-aarch64-standalone -o bbang

# Linux amd64
$ curl -sL https://github.com/eval/bbangsearch/releases/download/stable/bbang-linux-amd64-standalone -o bbang
```

## Usage

```shell
$ bbang -h
bbang v0.6.0

A CLI for DuckDuckGo's bang searches written in Babashka.

USAGE
  $ bbang [bang [& terms] [--url]]
  $ bbang [COMMAND]

OPTIONS
  --url  Print url instead of opening browser.

COMMANDS
  bangs:ls  List all bangs (or via `bbang bangs [& terms]`)

```

### Show me the ❗s

All roughly 14k bang searches from DuckDuckGo are included.

```shell
# print table
$ bbang bangs:ls

# grep, e.g. bangs from specific domain
$ bbang bangs:ls | grep -e '\.dk'

# Using DuckDuckGo's bang search
$ bbang bangs github
```

### Additional ❗s

Additional built-in bangs:

| Bang  | Description |
| ------------- | ------------- |
| `@gem` | Jump to gem on rubygems.org |
| `@rdoc` | Jump to gem on rubydoc.info |
| `cljd` | Alias for cljdoc |
| `drdk` | Denmark Radio (fixes default) |
| `drtv` | Denmark TV (fixes default) |
| `gem` | rubygems.org (jump-to via @gem) |
| `ghcclj` | Clojure code on GitHub (similar to `ghc`) |
| `ghclj`  | Clojure projects on GitHub (similar to `gh`)  |
| `ghdbf` | GitHub feed (no search) |
| `ghrel` | Visit/search GitHub releases (org/project detect like ghrepo) |
| `ghrepo` | Visit/search repo on GitHub (see doc below) |
| `grep` | Grep.app |
| `grepclj` | Grep.app for Clojure code |
| `java19` | Java v19 docs |
| `java20` | Java v20 docs |
| `java21` | Java v21 docs |
| `java` | Alias of java21 |
| `pgdoc14` | Postgresql docs v14 |
| `pgdoc15` | Postgresql docs v15 |
| `pgdoc16` | Postgresql docs v16 |
| `pgdoc` | Postgresql docs current version |
| `rdoc` | rubydoc.info, gems only (fixes default, jump-to via @gem) |

#### ghrepo

This bang deserves it's own paragraph as it's quite powerful.  

In its simplest form it allows for visiting/searching a GitHub repository you pass it:
```shell
# visit
$ bbang ghrepo eval/bbangsearch

# search
$ bbang ghrepo eval/bbangsearch some issue
```

It also has some implied defaults.  
E.g. with the right settings, you can leave out the GitHub organization and only provide a project-name:
```shell
# visit
$ bbang ghrepo bbangsearch

# search
$ bbang ghrepo bbangsearch some issue
```

bbang derives the GitHub organization from the following settings (in order of precedence):
* env-var `BBANG_GITHUB_ORG`
* env-var `GITHUB_ORG`
* git setting `github.org`  
  `$ git config --global github.org mycom`  (leave out `--global` to set it for the current repos).
* set env-var `BBANG_GITHUB_USER`
* set env-var `GITHUB_USER`
* git setting `github.user`  
  `$ git config --global github.user eval` (leave out `--global` to set it for the current repos).

Finally, when in a git working directory that has a remote pointing to GitHub[^1], you neither need to provide org or project:
```
# visit
$ bbang ghrepo

# search
$ bbang ghrepo _ some issue
```
[^1]: in order of preference: the origin-url, any remote with an ssh-url

### Custom ❗s

Custom bangs will be read from `~/.config/bbang/bangs.edn` (or `$XDG_CONFIG_HOME/bbang/bangs.edn`).  
Examples:
```clojure
{
  "mybang"        {:desc "My Project notifications"
                   :tpl  "https://github.com/notifications?query=repo%3Aeval%2Fbbangsearch+{{s|urlescape}}"}
}
```

The templating system used is [Selmer](https://github.com/yogthos/Selmer/). `s` will be set to whatever is searched for, e.g. `some "exact sentence"` when executing `bbang mybang some "exact sentence"`.
Custom bangs take precedence over built-in bangs. This e.g. allows for overriding defunct bangs.

Using Selmer's tags you can do nifty things:
```clojure
{
  "mybang"          {:desc "Some bang"
                     :tpl "{% if s|empty? %}https://example.org/all{% else %}https://example.org/search?q={{s|urlescape}}{% endif %}"}
  "project/tickets" {:desc "Visit/search tickets"
                     :tpl "{% ifmatches #\"^PROJ-\" s %}https://tickets.com/show/{{s}}{% else %}https://tickets.com/search?q={{s|urlescape}}{% endifmatches %}"
  "java19"          {:aliases ["java"]}}
}
```
The first example shows how to distinguish between executing `bbang mybang` and `bbang mybang some query` using Selmer's [if-tag](https://github.com/yogthos/Selmer/#if).
The second example shows how to distinguish between jumping to a known ticket (e.g. `PROJ-123`) and doing a search. It uses the bbang-specific `ifmatches`-tag.  
The last example shows how to alias an existing bang. Both `java19` and `java` exist (resp. search java docs v19 and (currently) v21). This alias ensures that `java` is equivalent to `java19`.

## License

Copyright (c) 2023 Gert Goet, ThinkCreate. Distributed under the MIT license. See [LICENSE](./LICENSE).
