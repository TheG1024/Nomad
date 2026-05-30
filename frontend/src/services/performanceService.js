/**
 * Service for monitoring and improving application performance
 */
class PerformanceService {
  constructor() {
    this.metrics = {
      firstPaint: null,
      firstContentfulPaint: null,
      domLoaded: null,
      windowLoaded: null,
      timeToInteractive: null,
      longTasks: [],
    };
    
    this.observers = [];
    this.isMonitoring = false;
  }
  
  /**
   * Start monitoring performance metrics
   */
  startMonitoring() {
    if (this.isMonitoring) return;
    this.isMonitoring = true;
    
    // Monitor page load metrics
    this.monitorPageLoad();
    
    // Monitor long tasks
    this.monitorLongTasks();
    
    // Monitor Core Web Vitals if available
    this.monitorWebVitals();
  }
  
  /**
   * Monitor basic page load metrics
   */
  monitorPageLoad() {
    // Use Performance API if available
    if (!window.performance) return;
    
    // Get navigation timing data
    const performanceEntries = performance.getEntriesByType('navigation');
    if (performanceEntries.length > 0) {
      const navigationEntry = performanceEntries[0];
      this.metrics.domLoaded = navigationEntry.domContentLoadedEventEnd;
      this.metrics.windowLoaded = navigationEntry.loadEventEnd;
    }
    
    // Get paint timing data
    const paintEntries = performance.getEntriesByType('paint');
    paintEntries.forEach(entry => {
      if (entry.name === 'first-paint') {
        this.metrics.firstPaint = entry.startTime;
      } else if (entry.name === 'first-contentful-paint') {
        this.metrics.firstContentfulPaint = entry.startTime;
      }
    });
    
    // Log initial metrics
    console.debug('Performance Metrics:', { 
      firstPaint: this.formatTime(this.metrics.firstPaint),
      firstContentfulPaint: this.formatTime(this.metrics.firstContentfulPaint),
      domLoaded: this.formatTime(this.metrics.domLoaded),
      windowLoaded: this.formatTime(this.metrics.windowLoaded),
    });
  }
  
  /**
   * Monitor long tasks that might cause jank
   */
  monitorLongTasks() {
    if ('PerformanceObserver' in window) {
      try {
        const longTaskObserver = new PerformanceObserver(list => {
          const entries = list.getEntries();
          
          entries.forEach(entry => {
            // Log tasks longer than 50ms
            if (entry.duration > 50) {
              const task = {
                duration: entry.duration,
                timestamp: performance.now(),
                name: entry.name,
              };
              
              this.metrics.longTasks.push(task);
              console.debug(`Long Task Detected: ${task.duration.toFixed(2)}ms`);
            }
          });
        });
        
        longTaskObserver.observe({ entryTypes: ['longtask'] });
        this.observers.push(longTaskObserver);
      } catch (e) {
        console.warn('Long Task Observer not supported', e);
      }
    }
  }
  
  /**
   * Monitor Core Web Vitals if available
   */
  monitorWebVitals() {
    if ('PerformanceObserver' in window) {
      try {
        // LCP - Largest Contentful Paint
        const lcpObserver = new PerformanceObserver(list => {
          const entries = list.getEntries();
          const lastEntry = entries[entries.length - 1];
          
          if (lastEntry) {
            this.metrics.largestContentfulPaint = lastEntry.startTime;
            console.debug(`LCP: ${this.formatTime(lastEntry.startTime)}`);
          }
        });
        
        lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });
        this.observers.push(lcpObserver);
        
        // FID - First Input Delay
        const fidObserver = new PerformanceObserver(list => {
          const entries = list.getEntries();
          entries.forEach(entry => {
            this.metrics.firstInputDelay = entry.processingStart - entry.startTime;
            console.debug(`FID: ${this.formatTime(this.metrics.firstInputDelay)}`);
          });
        });
        
        fidObserver.observe({ entryTypes: ['first-input'] });
        this.observers.push(fidObserver);
        
        // CLS - Cumulative Layout Shift
        if ('LayoutShift' in window) {
          let cumulativeLayoutShift = 0;
          
          const clsObserver = new PerformanceObserver(list => {
            for (const entry of list.getEntries()) {
              // Only count layout shifts without recent user input
              if (!entry.hadRecentInput) {
                cumulativeLayoutShift += entry.value;
                this.metrics.cumulativeLayoutShift = cumulativeLayoutShift;
              }
            }
            console.debug(`CLS: ${cumulativeLayoutShift.toFixed(4)}`);
          });
          
          clsObserver.observe({ entryTypes: ['layout-shift'] });
          this.observers.push(clsObserver);
        }
      } catch (e) {
        console.warn('Web Vitals observation not fully supported', e);
      }
    }
  }
  
  /**
   * Format time in milliseconds to a readable format
   * @param {number} timeInMs - Time in milliseconds
   * @returns {string} Formatted time string
   */
  formatTime(timeInMs) {
    if (!timeInMs) return 'N/A';
    return `${timeInMs.toFixed(2)}ms`;
  }
  
  /**
   * Get current performance metrics
   * @returns {Object} Current performance metrics
   */
  getMetrics() {
    return { ...this.metrics };
  }
  
  /**
   * Apply performance optimizations
   */
  applyOptimizations() {
    // Optimize images using IntersectionObserver for lazy loading
    this.optimizeImages();
    
    // Optimize event listeners
    this.optimizeEventListeners();
    
    // Optimize CSS rendering
    this.optimizeCssRendering();
  }
  
  /**
   * Optimize images with lazy loading
   */
  optimizeImages() {
    // Use native lazy loading if available
    if ('loading' in HTMLImageElement.prototype) {
      document.querySelectorAll('img:not([loading])').forEach(img => {
        if (!img.hasAttribute('loading')) {
          img.setAttribute('loading', 'lazy');
        }
      });
    } else {
      // Implement IntersectionObserver fallback for older browsers
      if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              const img = entry.target;
              const dataSrc = img.getAttribute('data-src');
              
              if (dataSrc) {
                img.src = dataSrc;
                img.removeAttribute('data-src');
              }
              
              observer.unobserve(img);
            }
          });
        });
        
        document.querySelectorAll('img[data-src]').forEach(img => {
          imageObserver.observe(img);
        });
      }
    }
  }
  
  /**
   * Optimize event listeners by applying debounce and throttle
   */
  optimizeEventListeners() {
    // Make debounce and throttle available globally
    window.pDebounce = this.debounce;
    window.pThrottle = this.throttle;
  }
  
  /**
   * Create a debounced function
   * @param {Function} func - Function to debounce
   * @param {number} wait - Wait time in milliseconds
   * @returns {Function} Debounced function
   */
  debounce(func, wait = 100) {
    let timeout;
    
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }
  
  /**
   * Create a throttled function
   * @param {Function} func - Function to throttle
   * @param {number} limit - Limit in milliseconds
   * @returns {Function} Throttled function
   */
  throttle(func, limit = 100) {
    let inThrottle;
    
    return function executedFunction(...args) {
      if (!inThrottle) {
        func(...args);
        inThrottle = true;
        
        setTimeout(() => {
          inThrottle = false;
        }, limit);
      }
    };
  }
  
  /**
   * Optimize CSS rendering by reducing paints and reflows
   */
  optimizeCssRendering() {
    // Add CSS containment to complex components
    document.querySelectorAll('.map-container, .data-grid, .chart-container').forEach(el => {
      el.style.contain = 'content';
    });
    
    // Add will-change hints for animations
    document.querySelectorAll('.animated, .transition').forEach(el => {
      el.style.willChange = 'transform, opacity';
    });
  }
  
  /**
   * Stop monitoring performance
   */
  stopMonitoring() {
    this.observers.forEach(observer => {
      if (observer && typeof observer.disconnect === 'function') {
        observer.disconnect();
      }
    });
    
    this.observers = [];
    this.isMonitoring = false;
  }
}

// Export as singleton
const performanceService = new PerformanceService();
export default performanceService; 