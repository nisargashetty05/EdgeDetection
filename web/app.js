// Display current timestamp
function updateTimestamp() {
    var timestampElement = document.getElementById('timestamp');
    if (timestampElement) {
        var now = new Date();
        timestampElement.textContent = "Last updated: ".concat(now.toLocaleString());
    }
}
// Handle image loading
function setupImageHandler() {
    var img = document.getElementById('sampleImage');
    if (img) {
        img.onerror = function () {
            img.style.display = 'none';
            var container = img.parentElement;
            if (container) {
                var note = document.createElement('p');
                note.className = 'sample-note';
                note.textContent = 'Sample image not available. Save a frame from the Android app to display here.';
                note.style.color = '#999';
                container.appendChild(note);
            }
        };
    }
}
// Initialize on page load
document.addEventListener('DOMContentLoaded', function () {
    updateTimestamp();
    setupImageHandler();
    console.log('Edge Detection Web Viewer Loaded');
    console.log('Android App: Real-Time Edge Detection with OpenCV');
});
