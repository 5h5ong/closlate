# Closlate

Translate File & Directory. Just you want!

## Getting Started

`Closlate`는 파파고 번역을 활용합니다. 이를 위해 키와 시크릿이 필요합니다.

`https://developers.naver.com/main/` 에서 키와 시크릿을 발급 받고 아래와 같은 형식으로 `config.json`에 적어주세요.

```json
{
  "id": "abcdefg",
  "secret": "something secret"
}
```

`config.json`은 실행 파일과 같은 디렉토리에 만들어 주세요.

## Usage

```shell
java -jar clojure-file-translate {directory path} {language of file or directory name} {language to be translate}
```

```shell
[Example]
 java -jar closlate "/Users/.../Desktop/Test" en ko
```

- directory path : 번역할 디렉토리의 경로입니다.
- language : ko, en, ja ...
    - language of file or directory name : 파일, 디렉토리 이름의 언어
    - language to be translate : 번역되길 원하는 언어