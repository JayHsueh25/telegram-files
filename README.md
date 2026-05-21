<p align="center">
    <img src="./web/public/favicon.svg" align="center" width="30%">
</p>
<p align="center"><h1 align="center">Telegram Files</h1></p>
<p align="center">
	<em><code>A self-hosted Telegram file downloader for continuous, stable, and unattended downloads.</code></em>
</p>
<p align="center">
	<img src="https://img.shields.io/github/license/jarvis2f/telegram-files?style=default&logo=opensourceinitiative&logoColor=white&color=0080ff" alt="license">
	<img src="https://img.shields.io/github/last-commit/jarvis2f/telegram-files?style=default&logo=git&logoColor=white&color=0080ff" alt="last-commit">
	<img src="https://img.shields.io/github/v/release/jarvis2f/telegram-files?style=default&logo=git&logoColor=white&color=0080ff" alt="release">
    <a href="https://codecov.io/gh/jarvis2f/telegram-files" > 
        <img src="https://codecov.io/gh/jarvis2f/telegram-files/graph/badge.svg?token=Y4YN2W8ARV"/> 
    </a>
</p>
<br>

## 🔗 Table of Contents

- [📍 Overview](#-overview)
- [🧩 Screenshots](#-screenshots)
- [🚀 Getting Started](#-getting-started)
- [⌨️ Development](#️-development)
    - [☑️ Prerequisites](#-prerequisites)
    - [⚙️ Installation](#-installation)
- [📌 Project Roadmap](#-project-roadmap)
- [🔰 Contributing](#-contributing)
- [🎗 License](#-license)
- [🆗 FAQs](#-faqs)

---

## 📍 Overview

* Seamless file downloads from Telegram channels and groups
* Support for multiple Telegram accounts to manage and download files simultaneously
* Pause and resume downloads anytime, with automatic file transfer to designated destinations
* Instant preview of downloaded videos and images
* Fully responsive design with mobile-friendly access, Progressive Web App (PWA) support, and offline capabilities
* Easily fetch files from Telegram shared links
* Built-in single administrator login with protected web access and in-app credential rotation

---

## 🧩 Screenshots

<div align="center">
    <img src="./misc/preview-files-pc.gif" width="70%">
    <img src="./misc/preview-files-mobile.gif" width="17.6%">
</div>

<details closed>
<summary>More Screenshots</summary>
<div align="center">
    <img src="./misc/screenshot-3.png" align="center" style="width: 300px; height: 500px;">
    <img src="./misc/screenshot-4.png" align="center" style="width: 300px; height: 500px;">
</div>

<div align="center">
    <img src="./misc/screenshot.png" align="center" width="40%">
    <img src="./misc/screenshot-2.png" align="center" width="40%">
</div>
</details>

## 🚀 Getting Started

Before getting started with telegram-files, you should apply a telegram api id and hash. You can apply for it on
the [Telegram API](https://my.telegram.org/apps) page.

**Using `docker`**
&nbsp; [<img align="center" src="https://img.shields.io/badge/Docker-2CA5E0.svg?style={badge_style}&logo=docker&logoColor=white" />](https://www.docker.com/)

```shell
docker run -d \
  --name telegram-files \
  --restart always \
  -e APP_ENV=${APP_ENV:-prod} \
  -e APP_ROOT=${APP_ROOT:-/app/data} \
  -e TELEGRAM_API_ID=${TELEGRAM_API_ID} \
  -e TELEGRAM_API_HASH=${TELEGRAM_API_HASH} \
  -p 6543:80 \
  -v ./data:/app/data \
  ${DOCKERHUB_IMAGE:-jarvis2f/telegram-files:latest}
```

**Using `docker-compose`**

Copy [docker-compose.yaml](docker-compose.yaml) and [.env.example](.env.example) to your project directory and run the following command:

```sh
docker-compose up -d
```

### 配置说明

部署时建议先复制 [.env.example](.env.example) 为 `.env`，按需修改配置后再执行 `docker-compose up -d`。
下面是常用配置项说明，详细解释见表格下方。

| 配置项 | 是否必填 | 示例 | 中文说明 |
| --- | --- | --- | --- |
| `TELEGRAM_API_ID` | 是 | `123456` | Telegram API ID，从 [Telegram API](https://my.telegram.org/apps) 申请。 |
| `TELEGRAM_API_HASH` | 是 | `abcdef123456` | Telegram API HASH，和 API ID 配套使用。 |
| `APP_ENV` | 否 | `prod` | 应用运行环境，Docker 部署保持 `prod`。 |
| `APP_ROOT` | 否 | `/app/data` | 容器内数据目录，默认映射到宿主机 `./data`。 |
| `ADMIN_USERNAME` | 否 | `admin` | 首次初始化管理员账号，数据库已有管理员凭据后不再覆盖。 |
| `ADMIN_PASSWORD` | 否 | `admin` | 首次初始化管理员密码，生产环境请在首次启动前修改。 |
| `DOCKERHUB_IMAGE` | 否 | `jayhsueh25/telegram-files:latest` | 使用自己 fork 发布的 Docker Hub 镜像时填写。 |
| `PUID` / `PGID` | 否 | `1000` | 指定容器内运行用户，常用于解决挂载目录权限问题。 |
| `NGINX_PORT` | 否 | `80` | 容器内 Nginx 监听端口，通常不需要改。 |
| `DB_TYPE` | 否 | `postgres` | 默认使用 SQLite；需要外部数据库时填写 `postgres` 或 `mysql`。 |
| `DB_HOST` / `DB_PORT` | 否 | `localhost` / `5432` | 外部数据库地址和端口，仅使用 PostgreSQL/MySQL 时需要。 |
| `DB_USER` / `DB_PASSWORD` | 否 | `postgres` / `postgres` | 外部数据库账号密码，仅使用 PostgreSQL/MySQL 时需要。 |
| `DB_NAME` | 否 | `telegram-files` | 外部数据库名称，仅使用 PostgreSQL/MySQL 时需要。 |
| `TELEGRAM_LOG_LEVEL` | 否 | `0` | TDLib 日志级别，数字越大日志越详细。 |
| `OPENAI_API_KEY` | 否 | `sk-...` | OpenAI API Key，仅使用 AI 相关能力时填写。 |
| `OPENAI_MODEL` | 否 | `gpt-4o-mini` | OpenAI 模型名称，留空使用默认值。 |
| `OPENAI_BASE_URL` | 否 | `https://api.openai.com/v1` | OpenAI API 地址，使用兼容服务时再修改。 |

配置项解释：

- `TELEGRAM_API_ID` 和 `TELEGRAM_API_HASH` 是启动服务的必填项，没有这两个值服务会启动失败。
- `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 只在首次初始化时生效。登录后请在右上角“账号安全”里修改账号和密码。
- 默认数据库是 SQLite，数据会存放在 `APP_ROOT` 下；普通个人部署不需要配置 PostgreSQL 或 MySQL。
- `DOCKERHUB_IMAGE` 用于切换镜像来源。如果你的 fork 发布到了自己的 Docker Hub 命名空间，就把它改成自己的镜像名。
- `PUID` / `PGID` 主要用于 Linux、NAS、unRaid 等环境，避免容器写入 `./data` 后宿主机用户无法读写。
- `NGINX_PORT` 是容器内部端口。一般保持 `80`，对外访问端口改 `docker-compose.yaml` 里的 `ports` 左侧，例如 `6543:80`。
- OpenAI 相关配置可以全部留空，只有使用 AI 转存、AI 分类等能力时才需要填写。

If your fork publishes under a different Docker Hub namespace, set `DOCKERHUB_IMAGE` in the copied `.env` file, for
example `jayhsueh25/telegram-files:latest`.

For GitHub Actions based publishing, configure these repository settings before pushing to `main`:

- `Secrets.DOCKERHUB_TOKEN`: Docker Hub access token with image push permission
- `Variables.DOCKERHUB_USERNAME`: Docker Hub namespace or username for the published image

After configuration, pushes to `main`, pushes of tags matching `v*`, and manual runs of
`.github/workflows/docker-publish.yml` from `main` or a version tag will publish a multi-architecture image to Docker
Hub automatically.

**Install on unRaid**

On unRaid, install from the Community Repositories by searching for `telegram-files`.

> **Important Note:** You should NOT expose the service to the public internet. Because the service is not secure.

### Admin Login

- The web UI is protected by a single administrator account.
- Default credentials are `admin / admin` on first startup only.
- Change the administrator username and password immediately after the first login from the top-right `账号安全` entry.
- The bootstrap credentials can be overridden with `ADMIN_USERNAME` and `ADMIN_PASSWORD` before the first startup.

---

## ⌨️ Development

### ☑️ Prerequisites

Before getting started with telegram-files, ensure your runtime environment meets the following requirements:

- **Programming Language:** JDK23,TypeScript
- **Package Manager:** Gradle,Npm
- **Container Runtime:** Docker

### ⚙️ Installation

Install telegram-files using one of the following methods:

**Build from source:**

1. Clone the telegram-files repository:

```sh
git clone https://github.com/jarvis2f/telegram-files
```

2. Navigate to the project directory:

```sh
cd telegram-files
```

3. Install the project dependencies:

**Using `npm`**
&nbsp; [<img align="center" src="https://img.shields.io/badge/npm-CB3837.svg?style={badge_style}&logo=npm&logoColor=white" />](https://www.npmjs.com/)

```sh
cd web
npm install
```

**Using `gradle`**
&nbsp; [<img align="center" src="https://img.shields.io/badge/Gradle-02303A.svg?style={badge_style}&logo=gradle&logoColor=white" />](https://gradle.org/)

```sh
cd api
gradle build
```

**Using `docker`**
&nbsp; [<img align="center" src="https://img.shields.io/badge/Docker-2CA5E0.svg?style={badge_style}&logo=docker&logoColor=white" />](https://www.docker.com/)

```sh
docker build -t jarvis2f/telegram-files .
```

## 📌 Project Roadmap

- ✅ **`Task 1`**: Automatically download files based on set rules.
- ✅ **`Task 2`**: Download statistics and reports.
- ✅ **`Task 3`**: Improve Telegram’s login functionality.
- ✅ **`Task 4`**: Support auto transfer files to other destinations.
- ✅ **`Task 5`**: File table is optimized using virtual lists.
- ✅ **`Task 6`**: Preload file information to support responsible searches.

---

## 🔰 Contributing

- **💬 [Join the Discussions](https://github.com/jarvis2f/telegram-files/discussions)**: Share your insights, provide
  feedback, or ask questions.
- **🐛 [Report Issues](https://github.com/jarvis2f/telegram-files/issues)**: Submit bugs found or log feature requests
  for the `telegram-files` project.
- **💡 [Submit Pull Requests](https://github.com/jarvis2f/telegram-files/blob/main/CONTRIBUTING.md)**: Review open PRs,
  and submit your own PRs.

<details closed>
<summary>Contributing Guidelines</summary>

1. **Fork the Repository**: Start by forking the project repository to your github account.
2. **Clone Locally**: Clone the forked repository to your local machine using a git client.
   ```sh
   git clone https://github.com/jarvis2f/telegram-files
   ```
3. **Create a New Branch**: Always work on a new branch, giving it a descriptive name.
   ```sh
   git checkout -b new-feature-x
   ```
4. **Make Your Changes**: Develop and test your changes locally.
5. **Commit Your Changes**: Commit with a clear message describing your updates.
   ```sh
   git commit -m 'Implemented new feature x.'
   ```
6. **Push to github**: Push the changes to your forked repository.
   ```sh
   git push origin new-feature-x
   ```
7. **Submit a Pull Request**: Create a PR against the original project repository. Clearly describe the changes and
   their motivations.
8. **Review**: Once your PR is reviewed and approved, it will be merged into the main branch. Congratulations on your
   contribution!

</details>

---

## 🎗 License

This project is protected under the MIT License. For more details,
refer to the [LICENSE](LICENSE) file.

---

## 🆗 FAQs

~~**Q.** Can't start the api server, error：`java.lang.UnsatisfiedLinkError: no tdjni in java.library.path`~~

~~**A.** Maybe download tdlib failed, you can see the [entrypoint.sh](entrypoint.sh) file, then download tdlib
manually.~~

**Q.** Web's spoiler is static, how to solve it?

**A.** 1. Because `CSS Houdini Paint API` is not supported by all browsers. 2. It is only supported on https.
<details closed>
<summary>Use in http environment, you can use the following method to solve it</summary>

Open the `chrome://flags` page, search for `Insecure origins treated as secure`, and add the address of the web page to
the list.
</details>

**Q.** How to use the telegram-files maintenance tool?

**A.** You can use the following command to run the maintenance tool(**before running the command, you should stop telegram-files container**):
<details closed>
<summary>Command</summary>

```shell
docker run --rm \
  --entrypoint tfm \
  -v $(pwd)/data:/app/data \
  -e APP_ROOT=${APP_ROOT:-/app/data} \
  -e TELEGRAM_API_ID=${TELEGRAM_API_ID} \
  -e TELEGRAM_API_HASH=${TELEGRAM_API_HASH} \
  ${DOCKERHUB_IMAGE:-jarvis2f/telegram-files:latest} ${Maintenance Command}
```

**Maintenance Command:**

- `album-caption`: Fixed issue with missing caption for album messages before `0.1.15`.
- `thumbnail`: Fixed issue with missing clear thumbnail.
</details>
