// Display current timestamp
function updateTimestamp(): void {
    const timestampElement = document.getElementById('timestamp');
    if (timestampElement) {
        const now = new Date();
        timestampElement.textContent = `Last updated: ${now.toLocaleString()}`;
    }
}

// Handle image loading
function setupImageHandler(): void {
    const img = document.getElementById('sampleImage') as HTMLImageElement;
    if (img) {
        img.onerror = () => {
            img.style.display = 'none';
            const container = img.parentElement;
            if (container) {
                const note = document.createElement('p');
                note.className = 'sample-note';
                note.textContent = 'Sample image not available. Save a frame from the Android app to display here.';
                note.style.color = '#999';
                container.appendChild(note);
            }
        };
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    updateTimestamp();
    setupImageHandler();
    
    console.log('Edge Detection Web Viewer Loaded');
    console.log('Android App: Real-Time Edge Detection with OpenCV');
});