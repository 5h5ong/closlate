# Closlate

Translate File & Directory. Just you want!

## Getting Started

`Closlate`는 DeepL 번역을 활용합니다. 이를 위해 api키가 필요합니다.

DeepL에서 api키를 발급 받고 아래와 같은 형식으로 `config.json`에 적어주세요.

```json
{
  "deepl-key": "..."
}
```

`config.json`은 실행 파일과 같은 디렉토리에 만들어 주세요.

## Usage

```shell
java "-Dfile.encoding=UTF-8" -jar ./closlate {directory path} {language to be translate}
```

```shell
[Example]
# Linux-like or Unix-like?
java "-Dfile.encoding=UTF-8" -jar closlate "/Users/.../Desktop/Test" ko

# Windows
java "-Dfile.encoding=UTF-8" -jar closlate.exe "C:\Users\...\Desktop\Test" ko
```

- directory path
    - 번역할 디렉토리의 경로
- language : ko, en, ja ...
    - language to be translate : 번역되길 원하는 언어
