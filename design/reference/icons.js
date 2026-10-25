// Tivi Home icon set as inline SVG (24x24). Line icons use currentColor with a 1.8 stroke and round caps;
// weather icons are filled and keep their own colours. Usage: <span data-icon="lock"></span>
const ICONS = {
  lock: '<rect x="5.5" y="11" width="13" height="9.5" rx="2"/><path d="M8.5 11V8a3.5 3.5 0 0 1 7 0v3"/><circle cx="12" cy="15.6" r="1.2" fill="currentColor" stroke="none"/>',
  wifi: '<path d="M3 9.5a13 13 0 0 1 18 0"/><path d="M6.2 12.8a8.5 8.5 0 0 1 11.6 0"/><path d="M9.4 16a4 4 0 0 1 5.2 0"/><circle cx="12" cy="19" r="1.1" fill="currentColor" stroke="none"/>',
  youtube: '<rect x="4.5" y="7" width="15" height="10.5" rx="2.5"/><path d="M10.5 9.8v5l4-2.5z" fill="currentColor" stroke="none"/>',
  store: '<rect x="5" y="9" width="14" height="12" rx="1.8"/><path d="M8.6 9a3.4 3.4 0 0 1 6.8 0"/><path d="M10.6 12.5v4.6l3.8-2.3z" fill="currentColor" stroke="none"/>',
  gear: '<circle cx="12" cy="12" r="4.8"/><circle cx="12" cy="12" r="1.7"/><g stroke-width="2.4"><path d="M19.4 12h1.7M17.2 17.2l1.2 1.2M12 19.4v1.7M6.8 17.2l-1.2 1.2M4.6 12H2.9M6.8 6.8 5.6 5.6M12 4.6V2.9M17.2 6.8l1.2-1.2"/></g>',
  grid: '<rect x="5" y="5" width="6" height="6" rx="1.3"/><rect x="13" y="5" width="6" height="6" rx="1.3"/><rect x="5" y="13" width="6" height="6" rx="1.3"/><rect x="13" y="13" width="6" height="6" rx="1.3"/>',
  palette: '<circle cx="12" cy="12" r="8.2"/><circle cx="14.4" cy="15.6" r="1.7"/><g fill="currentColor" stroke="none"><circle cx="8.4" cy="11" r="1.2"/><circle cx="11" cy="7.7" r="1.2"/><circle cx="15.1" cy="9.4" r="1.2"/></g>',
  play: '<path d="M8 6v12l10-6z" fill="currentColor" stroke="none"/>',
  music: '<path d="M9 17V6l10-2v11"/><circle cx="7" cy="17" r="2"/><circle cx="17" cy="15" r="2"/>',
  photo: '<rect x="4" y="5" width="16" height="14" rx="2"/><circle cx="9" cy="10" r="1.6"/><path d="m5 18 5-5 3 3 2-2 4 4"/>',
  globe: '<circle cx="12" cy="12" r="8"/><path d="M4 12h16M12 4c2.5 2.5 2.5 13.5 0 16M12 4c-2.5 2.5-2.5 13.5 0 16"/>',
  news: '<rect x="4" y="5" width="16" height="14" rx="2"/><path d="M7.5 9h9M7.5 12h9M7.5 15h5"/>',
  // weather (filled)
  sunCloud: '<g fill="#FFC53D" stroke="#FFC53D" stroke-width="0.9"><circle cx="8.9" cy="8.6" r="3.4" stroke="none"/><path d="M8.9 2.9v1.2M3.2 8.6h1.2M4.9 4.6l.9.9M12.9 4.6l-.9.9M4.9 12.6l.9-.9"/></g><g fill="#F2F5FA"><circle cx="9.6" cy="15.4" r="3.6"/><circle cx="13.6" cy="13" r="4.8"/><circle cx="17.6" cy="15.7" r="3.2"/><rect x="5.4" y="14.6" width="15.4" height="4.6" rx="2.3"/></g>',
  moonCloud: '<path d="M10.6 3.6a5 5 0 1 0 4.2 7.6 4.2 4.2 0 0 1-4.2-7.6z" fill="#F6E7A6"/><g fill="#F2F5FA"><circle cx="9.6" cy="15.4" r="3.6"/><circle cx="13.6" cy="13" r="4.8"/><circle cx="17.6" cy="15.7" r="3.2"/><rect x="5.4" y="14.6" width="15.4" height="4.6" rx="2.3"/></g>',
  rain: '<g fill="#D3DAE6"><circle cx="8.4" cy="10.6" r="3.6"/><circle cx="12.6" cy="8.2" r="4.8"/><circle cx="16.6" cy="10.8" r="3.2"/><rect x="4.4" y="9.8" width="15.4" height="4.6" rx="2.3"/></g><g stroke="#6CB8FF" stroke-width="1.5" stroke-linecap="round"><path d="M8.4 16.4l-.9 2.6M12.4 16.4l-.9 2.6M16.4 16.4l-.9 2.6"/></g>',
  sun: '<g fill="#FFC53D" stroke="#FFC53D" stroke-width="1.2" stroke-linecap="round"><circle cx="12" cy="12" r="4.4" stroke="none"/><path d="M12 3.4v1.8M12 18.8v1.8M3.4 12h1.8M18.8 12h1.8M5.9 5.9l1.3 1.3M16.8 16.8l1.3 1.3M5.9 18.1l1.3-1.3M16.8 7.2l1.3-1.3"/></g>'
};

function renderIcons(root = document) {
  root.querySelectorAll('[data-icon]').forEach(el => {
    el.innerHTML = `<svg class="i" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
      stroke-linecap="round" stroke-linejoin="round">${ICONS[el.getAttribute('data-icon')]}</svg>`;
  });
}
document.addEventListener('DOMContentLoaded', () => renderIcons());
