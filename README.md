# bbangsearch❗

A CLI for [DuckDuckGo's bang❗ searches](https://duckduckgo.com/bangs) written in [Babashka](https://babashka.org/).

## Rationale

I think bang searches are really great. Having DDG as my default search-engine, it's often easier to just type `!gh project` in the address bar than to cycle through open browser tabs to find the page of said GitHub project.

I found there's also some shortcomings:
- I often wish there would be a 'feeling lucky'-mode  
  often there's no need to search, but just jump to the first result.
- some bangs are no longer working
- option to add custom bangs
- easy access when working in the terminal

Enter `bbang`:
* open (or get the url of) any of the ~14k bang search pages via the commandline
* allow overriding/adding bangs
* list all bangs (in an easily grep-able format)
* allow for 'jump-to' functionality

Some examples:
```shell
# search all tickets (using a custom bang)
$ bbang proj/tickets some term

# visit ticket
$ bbang proj/tickets PROJ-123

# visit the GitHub page of the git repository you're working on
$ bbang ghrepo

# search it
$ bbang ghrepo _ some term

# visit another one of your repositories
$ bbang ghrepo deps-try
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

### standalone

TBD

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

## Usage

### ❗

```shell
# search GitHub
$ bbang gh bbangsearch

# open (Google's) search-page
$ bbang g

# just the url
$ bbang gh bbangsearch --url
```

### What ❗?

All roughly 14k bang searches from DuckDuckGo are included.

```shell
# print table
$ bbang bangs:ls

# bangs from specific domain
$ bbang bangs:ls | grep -e '\.dk'

# Using DuckDuckGo's bang search
$ bbang bangs github
```

### Additional ❗s

There's also a couple of additional bangs:

| Bang  | Description |
| ------------- | ------------- |
| `ghclj`  | Search Clojure projects on GitHub (similar to `gh`)  |
| `ghcclj` | Search Clojure code on GitHub (similar to `ghc`) |
| `grep` | Search Grep.app |
| `grepclj` | Search Grep.app for Clojure code |
| `rdoc` | Search rubydoc.info, gems only (fixes default bang) |
| `@rdoc` | Jump to gem on rubydoc.info |
| `@gem` | Jump to gem on rubygems.org |
| `ghrepo` | Visit/search repo on GitHub (see doc below) |

#### `ghrepo`

This bang distinguishes between visiting and searching a GitHub repo:
```shell
# visit repository
$ bbang ghrepo eval/deps-try

# search repository
$ bbang ghrepo eval/deps-try some term
```

It also has some implied defaults.  
When in a working directory that has a git remote pointing to GitHub[^1], you can visit or search the project:
```shell
# open GitHub project page
$ bbang ghrepo
# open GitHub search scoped to the current repository
$ bbang ghrepo _ some term
```

[^1]: remotes with urls of the form `git@github.com:org/project.git` take precedence over urls like `https://github.com/org/project.git`

With some extra settings you can also leave out the GitHub organisation:
```shell
# the GitHub org `eval` is implied
$ bbang ghrepo deps-try
```

Set the github-org like this (in order of precedence):
* set env-var `BBANG_GITHUB_ORG`
* set env-var `GITHUB_ORG`
* git setting `github.org`  
  `$ git config --global github.org mycom`  (leave out `--global` to set it for the current repos).
* set env-var `BBANG_GITHUB_USER`
* set env-var `GITHUB_USER`
* git setting `github.user`  
  `$ git config --global github.user eval` (leave out `--global` to set it for the current repos).

### Add custom ❗ searches

Custom bangs will be read from `~/.config/bbangsearch/bangs.edn` (or `$XDG_CONFIG_HOME/bbangsearch/bangs.edn`).  
Example:
```clojure
{
  "mybang"        {:desc "My Project notifications"
                   :tpl  "https://github.com/notifications?query=repo%3Aeval%2Fbbangsearch+{{s|urlescape}}"}
  "myotherbang"   {:desc "My Project PRs"
                   :tpl  "https://github.com/pulls?q=is%3Apr+archived%3Afalse+repo%3Aeval%2Fbbangsearch+sort%3Aupdated-desc+is%3Aopen+{{s|urlescape}}"}
}
```

The templating system used is [Selmer](https://github.com/yogthos/Selmer/). `s` will be set to whatever is searched for, e.g. `some "exact sentence"` when executing `bbang mybang some "exact sentence"`.
Custom bangs take precedence over existing bangs. This e.g. allows for overriding defunct bangs.  

Using Selmer's tags you can do nifty things:
```clojure
{
  "mybang"          {:desc "Some bang"
                     :tpl "{% if s|empty? %}https://example.org/all{% else %}https://example.org/search?q={{s|urlescape}}{% endif %}"}
  "project/tickets" {:desc "Visit/search tickets"
                     :tpl "{% ifmatches #\"^PROJ-\" s %}https://tickets.com/show/{{s}}{% else %}https://tickets.com/search?q={{s|urlescape}}{% endifmatches %}"}
}
```
The first example shows how to distinguish between executing `bbang mybang` and `bbang mybang some query` using Selmer's [if-tag](https://github.com/yogthos/Selmer/#if).  
The second example shows how to distinguish between jumping to a known ticket (e.g. `PROJ-123`) and doing a search. It uses the bbang-specific `ifmatches`-tag.

## License

Copyright (c) 2023 Gert Goet, ThinkCreate. Distributed under the MIT license. See [LICENSE](./LICENSE).
