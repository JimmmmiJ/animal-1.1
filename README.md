# 畜牧健康监测与预警管理平台离线部署版

本仓库用于在无法直接访问 Docker Hub 的 Windows 环境中部署和运行畜牧健康监测与预警管理平台。  
项目源码、Docker 编排文件和启动脚本存放在仓库中；运行所需的 Docker 镜像已打包为 `animal-2.0-images.tar`，请通过 GitHub Release 单独下载。

## 适用场景

- 目标电脑无法通过 `docker pull` 拉取 MySQL、Redis、Elasticsearch 或项目镜像。
- 目标电脑已安装 Docker Desktop。
- 需要通过离线镜像包快速启动完整系统。

## 环境要求

- Windows 10 / Windows 11
- Docker Desktop
- Git，可选；如未安装 Git，可通过 ZIP 方式下载源码

## 获取项目代码

推荐使用 Git 克隆仓库：

```powershell
git clone https://github.com/JimmmmiJ/animal-1.1.git
cd animal-1.1
```

如果目标电脑未安装 Git，也可以在 GitHub 仓库页面点击 `Code` -> `Download ZIP`，下载后解压到本地目录。

项目根目录不是固定的本机路径，而是代码下载或解压后的目录：

- 使用 `git clone` 时，项目根目录通常是 `animal-1.1`。
- 使用 `Download ZIP` 时，项目根目录通常是解压后的 `animal-1.1-main`。
- 判断标准是该目录下能看到 `docker-compose.yml`、`start-offline.bat` 和 `start-offline.ps1`。

## 下载离线镜像包

请从 Release 下载离线镜像包：

[animal-2.0-images.tar](https://github.com/JimmmmiJ/animal-1.1/releases/download/v1.1.0/animal-2.0-images.tar)

下载完成后，将 `animal-2.0-images.tar` 放到项目根目录，即与以下文件位于同一层级：

```text
animal-1.1/
├── animal-2.0-images.tar
├── docker-compose.yml
├── start-offline.bat
└── start-offline.ps1
```

## 启动系统

确认 Docker Desktop 已启动后，在项目根目录双击运行：

```text
start-offline.bat
```

也可以在 PowerShell 中执行：

```powershell
.\start-offline.ps1
```

启动脚本会自动检查本机 Docker 镜像是否完整。若缺少镜像，脚本会从项目根目录下的 `animal-2.0-images.tar` 执行 `docker load`，随后以离线方式启动所有服务。

## 访问系统

启动成功后，脚本窗口会输出登录地址。默认访问地址为：

```text
http://localhost/login
```

如果本机 80 端口已被占用，脚本会自动切换到其他可用端口，例如：

```text
http://localhost:8088/login
```

请以脚本窗口中 `Login:` 后显示的地址为准。

默认登录账号：

```text
用户名：admin
密码：admin123
```

## 常用命令

在项目根目录打开 PowerShell，可使用以下命令查看或停止服务。

查看容器状态：

```powershell
docker compose ps
```

查看运行日志：

```powershell
docker compose logs -f
```

停止系统：

```powershell
docker compose down
```

## 注意事项

- `animal-2.0-images.tar` 必须放在项目根目录，否则离线启动脚本无法自动加载镜像。
- 首次启动前请确认 Docker Desktop 已正常运行。
- 若浏览器无法打开系统，请查看启动脚本窗口输出的 `Login:` 地址。
- `animal-2.0-images.tar` 不提交到 Git 仓库本体，仅作为 GitHub Release 附件提供下载。
