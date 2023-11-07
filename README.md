# bbangsearch❗

A CLI for [DuckDuckGo's bang❗ searches](https://duckduckgo.com/bangs) written in [Babashka](https://babashka.org/).

## Installation

### homebrew

TBD

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
$ bbin install io.github.eval/bbangsearch --as bangbang
```

## Usage

### ❗

```shell
# search github
$ bbang gh bbangsearch

# open (Google's) search-page
$ bbang g

# just the url
$ bbang gh bbangsearch --url
```

### What ❗?

```shell
# print table
$ bbang bangs:ls

# bangs from specific domain
$ bbang bangs:ls | grep -e '\.dk'

# Using DuckDuckGo's bang search
$ bbang bangs github
```

### Add custom ❗ searches

Create a file `$XDG_CONFIG_HOME/bbangsearch/bangs.edn` (typically `~/.config/bbangsearch/bangs.edn`) with something like:
```clojure
{
  "mybang"   {:desc "My Project notifications"
              :tpl  "https://github.com/notifications?query=repo%3Aeval%2Fbbangsearch+{{s|urlescape}}"}
  "myotherbang"   {:desc "My Project PRs"
                   :tpl  "https://github.com/pulls?q=is%3Apr+archived%3Afalse+repo%3Aeval%2Fbbangsearch+sort%3Aupdated-desc+is%3Aopen+{{s|urlescape}}"}
}
```

Custom bangs take precedence over existing bangs.

## License

Copyright (c) 2023 Gert Goet, ThinkCreate. Distributed under the MIT license. See [LICENSE](./LICENSE).
