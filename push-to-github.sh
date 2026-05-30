#!/bin/bash
# Quick push script for Nomad repo

cd /home/tsugiri/Desktop/Nomad

echo "📦 Pushing to GitHub..."
echo ""
echo "Option 1: Use GitHub Personal Access Token"
echo "  1. Go to: https://github.com/settings/tokens"
echo "  2. Create token with 'repo' scope"
echo "  3. Run: git remote set-url origin https://YOUR_TOKEN@github.com/TheG1024/Nomad.git"
echo "  4. Run: git push origin main"
echo ""
echo "Option 2: Use SSH (if you have SSH keys)"
echo "  git remote set-url origin git@github.com:TheG1024/Nomad.git"
echo "  git push origin main"
echo ""
echo "Option 3: Use GitHub CLI"
echo "  gh auth login"
echo "  gh repo push"
echo ""
echo "Current remote:"
git remote -v
echo ""
echo "Latest commit:"
git log --oneline -1