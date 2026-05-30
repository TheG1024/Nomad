# Nomad GPS Tracker - Deployment Guide

## 🐳 Docker

```bash
docker build -t nomad-gps .
docker run -p 8080:8080 nomad-gps
```

## ☁️ Render

1. Visit: https://render.com/deploy?repo=https://github.com/TheG1024/Nomad
2. Configure Java service
3. Deploy!

## 🚂 Railway

```bash
railway init
railway up
```
