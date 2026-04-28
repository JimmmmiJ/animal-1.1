# 畜牧健康监测与预警管理平台离线版

这个仓库是给网络不能访问 Docker Hub 的电脑使用的离线运行版。  
不用在目标电脑上 `docker pull`，只需要下载源码和离线镜像包 `animal-2.0-images.tar`。

## 给朋友的使用步骤

1. 安装并打开 Docker Desktop。
2. 下载本仓库代码。推荐用 `git clone`：

```powershell
git clone https://github.com/JimmmmiJ/animal-1.1.git
cd animal-1.1
```

如果电脑上没有 Git，也可以在 GitHub 页面点 `Code` -> `Download ZIP`，下载后解压。

3. 下载离线镜像包：  
   [animal-2.0-images.tar](https://github.com/JimmmmiJ/animal-1.1/releases/download/v1.1.0/animal-2.0-images.tar)
4. 把 `animal-2.0-images.tar` 放到项目根目录。

项目根目录就是能看到这些文件的位置：

```text
start-offline.bat
start-offline.ps1
docker-compose.yml
animal-2.0-images.tar
```

5. 双击运行：

```text
start-offline.bat
```

也可以在 PowerShell 里运行：

```powershell
.\start-offline.ps1
```

脚本会自动检查本机是否已经有需要的 Docker 镜像。  
如果缺少镜像，会自动从根目录里的 `animal-2.0-images.tar` 执行 `docker load`，然后离线启动系统。

## 打开系统

启动成功后，脚本窗口会显示登录地址。通常是：

```text
http://localhost/login
```

如果电脑上的 80 端口已经被其他软件占用，脚本会自动换成其他端口，例如：

```text
http://localhost:8088/login
```

默认账号：

```text
用户名：admin
密码：admin123
```

## 常见问题

如果提示找不到镜像包，请检查 `animal-2.0-images.tar` 是否和 `start-offline.bat` 放在同一个文件夹。

如果 Docker 没有启动，请先手动打开 Docker Desktop，等 Docker Desktop 显示运行正常后，再重新双击 `start-offline.bat`。

如果启动后网页打不开，请看脚本窗口最后显示的 `Login:` 地址，以窗口里的地址为准。

## 停止系统

在项目根目录打开 PowerShell，运行：

```powershell
docker compose down
```

查看运行状态：

```powershell
docker compose ps
```

查看日志：

```powershell
docker compose logs -f
```

## 说明

这个仓库只放源码、Docker 配置和启动脚本。  
`animal-2.0-images.tar` 不放进 Git 仓库本体，而是放在 GitHub Release 里单独下载。
