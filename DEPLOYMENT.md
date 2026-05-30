# Nomad GPS Tracker - Deployment Guide

Quick deployment guides for cloud platforms.

## 🐳 Local Docker Testing

```bash
# Build and run locally
docker build -t nomad-gps .
docker run -p 8080:8080 nomad-gps

# Or with docker-compose
docker-compose up --build
```

Access at: http://localhost:8080

---

## ☁️ Render Deployment (Free Tier)

### Option 1: One-Click Deploy
[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/YOUR_USERNAME/Nomad)

### Option 2: Manual Setup

1. **Push to GitHub**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USERNAME/Nomad.git
   git push -u origin main
   ```

2. **Create Render Web Service**
   - Go to https://render.com
   - New → Web Service
   - Connect your GitHub repo
   - Settings:
     - **Build Command**: `mvn clean package`
     - **Start Command**: `java -jar target/*.jar`
     - **Environment**: Add `SPRING_PROFILES_ACTIVE=embedded`
   - Deploy!

3. **Get Your URL**
   - Render provides: `https://nomad-gps-xxx.onrender.com`
   - Free tier spins down after 15 min inactivity

---

## 🚂 Railway Deployment

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login
railway login

# Deploy
railway init
railway up
```

Railway auto-detects Spring Boot and deploys.

---

## 🪂 Fly.io Deployment

```bash
# Install Fly CLI
curl -L https://fly.io/install.sh | sh

# Login
fly auth login

# Create app
fly launch --name nomad-gps

# Deploy
fly deploy
```

---

## 🔧 Environment Variables

For production, set these environment variables:

```bash
# Redis (optional - app has in-memory fallback)
SPRING_REDIS_HOST=your-redis-host
SPRING_REDIS_PORT=6379

# Security (change from default!)
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=your-secure-password

# API Keys
OPENWEATHERMAP_API_KEY=your-api-key
```

---

## ✅ Post-Deployment Checklist

- [ ] Test health endpoint: `https://your-app.com/api/health`
- [ ] Verify WebSocket connection
- [ ] Test police alerts API
- [ ] Check device tracking works
- [ ] Update Browserbase to use production URL
- [ ] Change default admin password
- [ ] Set up monitoring/alerts

---

## 📊 Browserbase Testing

Once deployed, test with Browserbase:

```bash
smithery tool call browserbase start '{}'
smithery tool call browserbase navigate '{"url":"https://your-app.com"}'
smithery tool call browserbase extract '{"instruction":"Describe the dashboard"}'
```

---

## 🎯 Recommended: Render

**Why Render?**
- ✅ Free tier available
- ✅ Auto-deploys from GitHub
- ✅ Built-in HTTPS
- ✅ Easy environment variables
- ✅ Health checks
- ✅ Auto-scaling

**Deploy now:**
1. Push code to GitHub
2. Click "Deploy to Render" button
3. Wait 5 minutes
4. Get your public URL!