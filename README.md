# Clojure File Translate

입력된 디렉토리 자체와 그 안 모든 요소들의 이름을 번역합니다.

## Getting Started

`Clojure File Translate`는 파파고 번역을 활용하며 이를 위해 키와 시크릿이 필요합니다.

`https://developers.naver.com/main/` 에서 키와 시크릿을 발급받고 아래와 같은 형식으로 `config.json`에 적어주세요.

```json
{
  "id": "abcdefg",
  "secret": "something special"
}
```

`config.json`은 실행 파일과 같은 디렉토리에 만들어주면 됩니다.

## Usage

```shell
java -jar clojure-file-translate {directory path} {language to translate} {language to be translate}
```

```shell
[Example]
 java -jar clojure-file-translate "/Users/.../Desktop/Test" en ko
```

- directory path : 번역할 디렉토리의 경로입니다.
- language : ko, en, ja ...
    - language to translate : 번역할 언어
    - language to be translate : 번역될 언어