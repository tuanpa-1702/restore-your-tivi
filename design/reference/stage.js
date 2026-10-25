// Scales the 960x540 stage to fill the window, letterboxed, exactly like the TV app does.
function fitStage() {
  const stage = document.querySelector('.stage');
  const s = Math.min(innerWidth / 960, innerHeight / 540);
  stage.style.transform = `translate(${(innerWidth - 960 * s) / 2}px, ${(innerHeight - 540 * s) / 2}px) scale(${s})`;
}
addEventListener('resize', fitStage);
document.addEventListener('DOMContentLoaded', fitStage);
