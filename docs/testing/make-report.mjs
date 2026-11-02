// Tao docs/testing/test-report.md tu docs/testing/test-results.json
// Chay: node docs/testing/make-report.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const { runAt, baseUrl, results } = JSON.parse(readFileSync(join(dir, 'test-results.json'), 'utf8'));
const esc = (s) => String(s ?? '').replaceAll('|', '\\|').replaceAll('\n', ' ');

const modules = [...new Set(results.map((r) => r.module))];
const pass = results.filter((r) => r.result === 'PASS').length;

let md = `# Báo cáo kiểm thử chức năng SmartDining

- Thời điểm chạy: ${new Date(runAt).toLocaleString('vi-VN')}
- Môi trường: ${baseUrl} · Tomcat 10.1.20 · JDK 25 · SQL Server Express (DB \`Smart_Dinings\`)
- Phương pháp: kiểm thử hộp đen qua HTTP (gọi trang và API như trình duyệt), tự động bằng \`docs/testing/api-test-runner.mjs\`
- Tài khoản test: tài khoản mẫu trong \`database/SmartDining.sql\`
- Dữ liệu test: mọi bản ghi do test tạo có tên bắt đầu bằng \`QA \` và được tự động xoá trước và sau mỗi lần chạy (\`cleanup-test-data.sql\`); thêm \`--keep-data\` để giữ lại
- Kết quả: **${results.length} test case — ${pass} PASS — ${results.length - pass} FAIL**

## Tổng hợp theo module

| Module | Số TC | PASS | FAIL |
|---|---|---|---|
${modules.map((m) => {
    const list = results.filter((r) => r.module === m);
    const p = list.filter((r) => r.result === 'PASS').length;
    return `| ${m} | ${list.length} | ${p} | ${list.length - p} |`;
}).join('\n')}

`;

for (const m of modules) {
    md += `## ${m}\n\n| Mã TC | Tiêu đề | Dữ liệu / Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Đánh giá |\n|---|---|---|---|---|---|\n`;
    for (const r of results.filter((x) => x.module === m)) {
        md += `| ${r.id} | ${esc(r.title)} | ${esc(r.data)} | ${esc(r.expected)} | ${esc(r.actual).slice(0, 160)} | ${r.result} |\n`;
    }
    md += '\n';
}

md += readFileSync(join(dir, 'defects.md'), 'utf8');
writeFileSync(join(dir, 'test-report.md'), md);
console.log('Da tao docs/testing/test-report.md');
