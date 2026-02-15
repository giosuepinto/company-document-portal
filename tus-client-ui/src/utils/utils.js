// File: utils/utils.js

// Converts bytes into a readable format (KB, MB, GB)
export function formatBytes(bytes, decimals = 2) {
    if (!+bytes) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

// Formats ISO dates (e.g. "2025-10-26T19:25:17.836Z") into a readable format
export function formatDate(isoString) {
    if (!isoString) return 'N/A';
    try {
        // Use en-GB or en-US locale for English date format
        return new Date(isoString).toLocaleString('en-GB', {
            year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
    } catch (e) {
        return 'Invalid Date';
    }
}

// Helper per leggere il cookie CSRF impostato dal Gateway
export function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}