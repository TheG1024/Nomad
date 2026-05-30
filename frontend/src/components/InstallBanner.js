import React, { useState, useEffect } from 'react';
import pwaService from '../services/pwaService';

/**
 * Banner component that prompts users to install the app as a PWA
 */
const InstallBanner = () => {
  const [isVisible, setIsVisible] = useState(false);
  const [isInstalled, setIsInstalled] = useState(false);

  useEffect(() => {
    // Check if already installed
    pwaService.isAppInstalled().then(installed => {
      setIsInstalled(installed);
    });

    // Listen for app installable event
    const handleAppInstallable = () => {
      setIsVisible(true);
    };

    window.addEventListener('appinstallable', handleAppInstallable);

    // Cleanup
    return () => {
      window.removeEventListener('appinstallable', handleAppInstallable);
    };
  }, []);

  const handleInstall = async () => {
    try {
      const installed = await pwaService.showInstallPrompt();
      setIsVisible(false);
      if (installed) {
        setIsInstalled(true);
      }
    } catch (error) {
      console.error('Installation failed:', error);
    }
  };

  const handleDismiss = () => {
    setIsVisible(false);
    // Store preference in localStorage to avoid showing again in this session
    localStorage.setItem('installBannerDismissed', 'true');
  };

  // Don't show if already installed or dismissed
  if (isInstalled || !isVisible || localStorage.getItem('installBannerDismissed') === 'true') {
    return null;
  }

  return (
    <div className="install-banner">
      <div className="install-banner-content">
        <div className="install-banner-icon">
          <img src="/icons/icon-192x192.png" alt="GPS Tracker Icon" width="48" height="48" />
        </div>
        <div className="install-banner-text">
          <h3>Install GPS Tracker</h3>
          <p>Install this app on your device for offline access and better performance.</p>
        </div>
        <div className="install-banner-actions">
          <button onClick={handleInstall} className="install-button">Install</button>
          <button onClick={handleDismiss} className="dismiss-button">Not Now</button>
        </div>
      </div>
    </div>
  );
};

export default InstallBanner; 