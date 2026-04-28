# 畜牧健康监测与预警管理平台离线版

这是供朋友下载运行的干净发布版本。仓库只包含项目源码、Docker 配置和启动脚本，不包含汇报材料、构建产物、PDF 手册或离线镜像包。

## 运行前准备

1. 安装并打开 Docker Desktop。
2. 从本仓库的 GitHub Release 下载 `animal-2.0-images.tar`。
3. 把 `animal-2.0-images.tar` 放到本项目根目录，也就是和 `start-offline.bat` 同一层。

## 一键启动

在 Windows 上双击：

```text
start-offline.bat
```

也可以在 PowerShell 中运行：

```powershell
.\start-offline.ps1
```

脚本会检查本机 Docker 镜像；如果缺少镜像，会自动从项目根目录的 `animal-2.0-images.tar` 执行 `docker load`，然后以离线方式启动容器。

## 访问地址

启动成功后，脚本窗口会显示实际登录地址。通常是：

```text
http://localhost/login
```

如果本机 80 端口被占用，脚本会自动切换到其他端口，例如：

```text
http://localhost:8088/login
```

默认账号：

```text
用户名：admin
密码：admin123
```

## 常用命令

在项目根目录可以执行：

```powershell
docker compose ps
docker compose logs -f
docker compose down
```

## 重新生成离线镜像包

如果需要重新生成 `animal-2.0-images.tar`，请在一台可以访问 Docker Hub 的电脑上运行：

```powershell
.\export-offline-images.ps1
```

生成的文件位于：

```text
offline-images\animal-2.0-images.tar
```

这个 tar 文件不要提交到 Git 仓库，请通过 GitHub Release 或其他文件分享方式单独分发。
