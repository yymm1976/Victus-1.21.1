const fs = require('fs');
const path = require('path');

const oldId = 'victus-unofficial-neoforge-port';
const newId = 'victus';

function walk(dir, callback) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        if (fullPath.includes('.git') || fullPath.includes('.gradle') || fullPath.includes('build') || fullPath.includes('run')) {
            continue;
        }
        if (fs.statSync(fullPath).isDirectory()) {
            walk(fullPath, callback);
            callback(fullPath, true);
        } else {
            callback(fullPath, false);
        }
    }
}

walk('src', (p, isDir) => {
    if (isDir) {
        const name = path.basename(p);
        if (name === oldId) {
            fs.renameSync(p, path.join(path.dirname(p), newId));
            console.log(`Renamed directory ${p}`);
        }
    }
});

walk('.', (p, isDir) => {
    if (!isDir && (p.endsWith('.json') || p.endsWith('.java') || p.endsWith('.properties') || p.endsWith('.toml') || p.endsWith('.kts') || p.endsWith('.cfg'))) {
        try {
            const content = fs.readFileSync(p, 'utf-8');
            if (content.includes(oldId)) {
                fs.writeFileSync(p, content.split(oldId).join(newId), 'utf-8');
                console.log(`Updated file ${p}`);
            }
        } catch (e) {
            console.error(`Error processing ${p}:`, e.message);
        }
    }
});