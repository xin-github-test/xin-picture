<template>
  <main>
    <div class="card" onclick="this.classList.toggle('flipped')">
      <div class="card-face card-front">
        <!-- 背景阴影 -->
        <img class="overlay-edge" src="/src/assets/card/card_bg.jpg" alt="" />

        <!-- 点阵背景 -->
        <!-- <svg
          class="overlay-texture"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="xMaxYMax meet"
          viewBox="0 0 630 880"
        >
          <pattern
            id="card-texture"
            width="4"
            height="4"
            patternTransform="scale(3)"
            patternUnits="userSpaceOnUse"
          >
            <path fill="#000000" d="M1 3h1v1H1V3zm2-2h1v1H3V1z"></path>
          </pattern>
          <rect x="0" y="0" width="630" height="880" fill="url(#card-texture)" opacity=".1" />
        </svg> -->

        <h2 class="card-name">Dragon Egg</h2>

        <div
          class="card-art"
          role="img"
          aria-label="Multi-layered scene of a dragon egg that's sitting in a foliage heavy jungle environment."
        >
          <!-- Base Image -->
          <img class="display-img" src="/src/assets/card/10.png" alt="" />
          <!-- Hatch Ability: Egg and Crack -->
          <svg
            xmlns="https://www.w3.org/2000/svg"
            preserveAspectRatio="xMaxYMax meet"
            viewBox="0 0 590 380"
          >
            <defs>
              <pattern
                id="pattern-egg"
                width="20"
                height="20"
                patternTransform="rotate(170)scale(1.4)"
                patternUnits="userSpaceOnUse"
              >
                <rect width="100%" height="100%" fill="#adc39e" />
                <path
                  fill="none"
                  stroke="var(--color-dark-translucent)"
                  stroke-width="2"
                  d="M-10-10A10 10 0 0 0-20 0a10 10 0 0 0 10 10A10 10 0 0 1 0 0a10 10 0 0 0-10-10zm20 0A10 10 0 0 0 0 0a10 10 0 0 1 10 10A10 10 0 0 1 20 0a10 10 0 0 0-10-10zm20 0A10 10 0 0 0 20 0a10 10 0 0 1 10 10A10 10 0 0 1 40 0a10 10 0 0 0-10-10zm-40 20a10 10 0 0 0-10 10 10 10 0 0 0 10 10A10 10 0 0 1 0 20a10 10 0 0 0-10-10zm20 0A10 10 0 0 0 0 20a10 10 0 0 1 10 10 10 10 0 0 1 10-10 10 10 0 0 0-10-10zm20 0a10 10 0 0 0-10 10 10 10 0 0 1 10 10 10 10 0 0 1 10-10 10 10 0 0 0-10-10z"
                />
              </pattern>
            </defs>
          </svg>
          <!-- Light Rays -->
          <img
            style="width: 100%; height: 100%"
            class="rays"
            src="https://assets.codepen.io/2153413/dragonEgg-rays.png"
            alt=""
          />
          <!-- Study Ability: UI & Magnifying Glass -->
          <svg
            xmlns="https://www.w3.org/2000/svg"
            preserveAspectRatio="xMaxYMax meet"
            viewBox="0 0 590 380"
          >
            <path
              id="ability-study"
              fill="var(--color-light)"
              opacity="0"
              d="M22,24C20.896,24 20,23.104 20,22C20,20.896 20.896,20 22,20L136.731,20C137.835,20 138.731,20.896 138.731,22C138.731,23.104 137.835,24 136.731,24L22,24ZM22,34C20.896,34 20,33.104 20,32C20,30.896 20.896,30 22,30L287.906,30C289.01,30 289.906,30.896 289.906,32C289.906,33.104 289.01,34 287.906,34L22,34ZM22,44C20.896,44 20,43.104 20,42C20,40.896 20.896,40 22,40L176.58,40C177.684,40 178.58,40.896 178.58,42C178.58,43.104 177.684,44 176.58,44L22,44ZM187.729,44C186.625,44 185.729,43.104 185.729,42C185.729,40.896 186.625,40 187.729,40L269.114,40C270.218,40 271.114,40.896 271.114,42C271.114,43.104 270.218,44 269.114,44L187.729,44ZM22,54C20.896,54 20,53.104 20,52C20,50.896 20.896,50 22,50L211.47,50C212.573,50 213.47,50.896 213.47,52C213.47,53.104 212.573,54 211.47,54L22,54ZM22,64C20.896,64 20,63.104 20,62C20,60.896 20.896,60 22,60L228.421,60C229.525,60 230.421,60.896 230.421,62C230.421,63.104 229.525,64 228.421,64L22,64ZM22,84C20.896,84 20,83.104 20,82C20,80.896 20.896,80 22,80L155.63,80C156.734,80 157.63,80.896 157.63,82C157.63,83.104 156.734,84 155.63,84L22,84ZM177.12,84C176.016,84 175.12,83.104 175.12,82C175.12,80.896 176.016,80 177.12,80L215.695,80C216.799,80 217.695,80.896 217.695,82C217.695,83.104 216.799,84 215.695,84L177.12,84ZM236.586,84C235.483,84 234.586,83.104 234.586,82C234.586,80.896 235.483,80 236.586,80L264.372,80C265.476,80 266.372,80.896 266.372,82C266.372,83.104 265.476,84 264.372,84L236.586,84ZM22,94C20.896,94 20,93.104 20,92C20,90.896 20.896,90 22,90L192.901,90C194.005,90 194.901,90.896 194.901,92C194.901,93.104 194.005,94 192.901,94L22,94ZM22,104C20.896,104 20,103.104 20,102C20,100.896 20.896,100 22,100L127.793,100C128.897,100 129.793,100.896 129.793,102C129.793,103.104 128.897,104 127.793,104L22,104ZM141.553,104C140.449,104 139.553,103.104 139.553,102C139.553,100.896 140.449,100 141.553,100L207.453,100C208.557,100 209.453,100.896 209.453,102C209.453,103.104 208.557,104 207.453,104L141.553,104ZM452.9,358.667L452.9,283.367L464.9,283.367L464.9,357.333L473.479,357.333L473.479,245.411L485.479,245.411L485.479,357.333L494.058,357.333L494.058,283.367L506.058,283.367L506.058,357.333L514.636,357.333L514.636,320.683L526.636,320.683L526.636,357.333L535.215,357.333L535.215,264.065L547.215,264.065L547.215,357.333L555.794,357.333L555.794,299.449L567.794,299.449C567.794,299.449 568.162,358.667 567.794,358.667L452.9,358.667ZM71.224,336.972L71.224,358.667L20,358.667L20,336.972L71.224,336.972ZM130.448,336.972L130.448,358.667L79.224,358.667L79.224,336.972L130.448,336.972ZM189.671,336.972L189.671,358.667L138.448,358.667L138.448,336.972L189.671,336.972Z"
            />
            <g id="ability-study-glass" opacity="0">
              <path
                fill="var(--color-light)"
                d="m282.115 203.92 5.958 10.32-16.719 9.653-5.959-10.32 16.72-9.653Z"
              />
              <path
                fill="var(--color-dark)"
                d="m273.651 226.817-92.766 53.558a3.887 3.887 0 0 1-5.307-1.422l-5.754-9.966a3.888 3.888 0 0 1 1.422-5.308l92.766-53.558a3.887 3.887 0 0 1 5.307 1.422l5.754 9.966a3.888 3.888 0 0 1-1.422 5.308Z"
              />
              <path
                fill="var(--color-light)"
                style="mix-blend-mode: exclusion"
                d="M309.299 110.973c34.825-20.106 79.423-8.157 99.529 26.669 20.106 34.825 8.157 79.423-26.669 99.529-34.825 20.106-79.422 8.156-99.529-26.669-20.106-34.825-8.156-79.423 26.669-99.529Z"
              />
              <path
                fill="var(--color-dark)"
                d="M309.299 110.973c34.825-20.106 79.423-8.157 99.529 26.669 20.106 34.825 8.157 79.423-26.669 99.529-34.825 20.106-79.422 8.156-99.529-26.669-20.106-34.825-8.156-79.423 26.669-99.529Zm2 3.464c-32.913 19.003-44.207 61.152-25.205 94.065 19.003 32.914 61.152 44.207 94.065 25.205 32.914-19.003 44.208-61.152 25.205-94.065-19.003-32.914-61.152-44.207-94.065-25.205Z"
              />
              <path
                fill="none"
                stroke="var(--color-light)"
                stroke-width="4"
                d="M335.278 233.709c-17.034-2.997-32.676-13.224-41.991-29.359-5.514-9.55-8.127-19.983-8.114-30.278"
              />
              <path
                fill="none"
                stroke="var(--color-light)"
                stroke-width="4"
                d="M286.093 163.621a60.931 60.931 0 0 1 6.885-19.287"
              />
            </g>
          </svg>
        </div>

        <!-- 描述信息 -->
        <fieldset class="card-abilities" onclick="event.stopPropagation();">
          <label class="ability">
            <span>作者:</span>
            <p>admin</p>
          </label>
          <!-- <span>OR</span> -->
          <label class="ability">
            <span>尺寸:</span>
            <p><em>1024*2560</em></p>
          </label>
          <label class="ability">
            <span>标签:</span>
            <p><a-tag style="width: auto" color="green">默认</a-tag></p>
          </label>
          <label class="ability">
            <span>评分:</span>
            <p>
              <a-tag style="all: unset; color: wheat; border: 1px solid wheat">默认</a-tag>
            </p>
          </label>
        </fieldset>

        <div class="card-source">
          <p>上传时间：2025.03.28</p>
          <p>12/64</p>
        </div>
      </div>

      <div class="card-face card-back">
        <img src="https://assets.codepen.io/2153413/card-back.jpg" alt="" />
        <img class="overlay-edge" src="https://assets.codepen.io/2153413/card-edge.png" alt="" />
        <svg
          class="overlay-texture"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="xMaxYMax meet"
          viewBox="0 0 630 880"
        >
          <pattern
            id="card-texture"
            width="4"
            height="4"
            patternTransform="scale(3)"
            patternUnits="userSpaceOnUse"
          >
            <path fill="#000000" d="M1 3h1v1H1V3zm2-2h1v1H3V1z"></path>
          </pattern>
          <rect x="0" y="0" width="630" height="880" fill="url(#card-texture)" opacity=".25" />
        </svg>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
// Grab the svg path lengths, so we can animate them easily.
// These -could- be set directly into the CSS if you knew the values before hand, making this a CSS-only friendly approach to SVG animation

// Ability Hatch
// const abilityHatch = document.querySelector('#ability-hatch-crack')
// abilityHatch.style.strokeDasharray = abilityHatch.getTotalLength()
// abilityHatch.style.strokeDashoffset = abilityHatch.getTotalLength()
// // console.log(`Study Path = ${abilityHatch.getTotalLength()}`);

// // Helps dismiss the dialog/sidebar, allowing folks to click anywhere
// experimentDialog.addEventListener('click', ({ target: dialog }) => {
//   if (dialog.nodeName === 'DIALOG') {
//     dialog.close('dismiss')
//   }
// })
</script>

<style scoped>
@keyframes anim-stroke {
  to {
    stroke-dashoffset: 0;
  }
}

@keyframes anim-opacity {
  to {
    opacity: 1;
  }
}

@keyframes anim-hatch {
  0%,
  80% {
    transform: rotate(0deg);
  }
  10%,
  30%,
  50%,
  70% {
    transform: rotate(-2deg);
  }
  20%,
  40%,
  60% {
    transform: rotate(3deg);
  }
}

@keyframes anim-study {
  0% {
    transform: translateX(75px) translateY(0);
  }
  25% {
    transform: translateX(0) translateY(50px);
  }
  50% {
    transform: translateX(-75px) translateY(0);
  }
  75% {
    transform: translateX(0) translateY(-50px);
  }
  100% {
    transform: translateX(75px) translateY(0);
  }
}
/* 渐现效果 */
@keyframes fadeIn {
  0% {
    opacity: 0;
    transform: scale(0.5); /* 字体逐渐放大 */
  }
  100% {
    opacity: 1;
    transform: scale(1); /* 字体最终恢复正常大小 */
  }
}

/* 字体闪烁效果 */
@keyframes flicker {
  0% {
    text-shadow:
      2px 2px 5px rgba(0, 0, 0, 0.8),
      0 0 15px rgba(255, 0, 0, 0.6),
      0 0 30px rgba(255, 0, 0, 0.4),
      0 0 60px rgba(255, 0, 0, 0.2);
  }
  50% {
    text-shadow:
      2px 2px 5px rgba(0, 0, 0, 0.8),
      0 0 20px rgba(255, 0, 0, 0.9),
      0 0 40px rgba(255, 0, 0, 0.7),
      0 0 70px rgba(255, 0, 0, 0.4);
  }
  100% {
    text-shadow:
      2px 2px 5px rgba(0, 0, 0, 0.8),
      0 0 15px rgba(255, 0, 0, 0.6),
      0 0 30px rgba(255, 0, 0, 0.4),
      0 0 60px rgba(255, 0, 0, 0.2);
  }
}

.card {
  position: relative;
  /* background-image: url('/src/assets/card/card_bg.jpg');
  background-repeat: no-repeat;
  background-size: cover; */
  width: 350px;
  aspect-ratio: 6.3 / 8.8;
  /*使得子元素和父元素一起进行3D变换 */
  transform-style: preserve-3d;
  backface-visibility: hidden;
  overflow: hidden;
  border-radius: 1em;
  /* box-shadow: inset 0 0 40px rgba(68, 15, 202, 0.5); */
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
  transition:
    transform 0.5s ease,
    filter 0.2s ease;
  will-change: transform;
  -webkit-user-select: none;
  user-select: none;
  cursor: url('/src/assets/card/cursor_flip.webp'), auto;

  &:hover {
    transform: scale(1.1) rotate3d(1, 1, 1, 2deg);
    filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.5));

    .card-art .rays {
      opacity: 0.9;
    }
  }

  &:has(#ability-two:checked) {
    #ability-study {
      animation: anim-opacity 2.5s ease-out forwards;
      animation-iteration-count: 1;
    }

    #ability-study-glass {
      opacity: 1;
      animation: anim-study 10s ease-in-out infinite;
    }
  }
}

.display-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.card-face {
  position: absolute;
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  backface-visibility: hidden;
  border-radius: 1em;
  background-color: var(--color-light);
  background-image: var(--texture-noise);
  transition: transform 0.75s;
  transition-timing-function: linear(
    0,
    0.008 1.1%,
    0.031 2.2%,
    0.129 4.8%,
    0.257 7.2%,
    0.671 14.2%,
    0.789 16.5%,
    0.881 18.6%,
    0.957 20.7%,
    1.019 22.9%,
    1.063 25.1%,
    1.094 27.4%,
    1.114 30.7%,
    1.112 34.5%,
    1.018 49.9%,
    0.99 59.1%,
    1
  );
  z-index: 1;
}

.card-front {
  transform: rotateY(0deg);
  padding: 1em 1em 0.25em 1em;
}

.card-back {
  transform: rotateY(180deg);
  place-content: center;

  .overlay-edge {
    transform: rotateY(180deg);
    z-index: -1;
  }
}

.flipped > .card-front {
  transform: rotateY(-180deg);
}

.flipped > .card-back {
  transform: rotateY(0deg);
}

.card-name {
  position: relative;
  margin: unset;
  margin-bottom: 0.25em;
  font-size: var(--step-2);
  text-transform: uppercase;
  font-weight: normal;
}

.card-art {
  position: relative;
  width: 100%;
  height: auto;
  aspect-ratio: 5.9 / 3.8;
  overflow: hidden;
  border-radius: 0.5em;
  outline: solid 2px var(--color-dark);

  img,
  svg {
    position: absolute;
  }

  .rays {
    mix-blend-mode: overlay;
    transition: opacity 0.5s ease-in-out;
    opacity: 0;
  }
}

.card-source {
  font-size: var(--step--2);
  display: flex;
  justify-content: space-between;
}

.overlay-edge {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: -1;
  filter: brightness(40%);

  width: 100%;
  height: 100%;
}

.overlay-texture {
  position: absolute;
  inset: 0;
  mix-blend-mode: overlay;
  pointer-events: none;
  z-index: 100;
}

.card-abilities {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex-grow: 1;
  gap: 0.5em;
  padding: unset;
  border: unset;
  pointer-events: auto;
  -webkit-user-select: none;
  user-select: none;
  cursor: initial;
  z-index: 10;

  .ability {
    position: relative;
    display: flex;
    gap: 1em;
    justify-content: center;
    padding: 0.5em 0.75em;
    border-radius: 0.5em;
    outline: solid 1px var(--color-dark-25);
    transition: outline 0.25s ease-in-out;
    cursor: pointer;
    z-index: 11;

    &:hover {
      outline-color: var(--color-accent-lighter);
    }

    span {
      width: 15%;
      font-family: var(--font-headers);
      font-weight: normal;
      align-items: center;
      font-size: 18px;
      align-self: center;
      white-space: nowrap;
      color: wheat;
      text-shadow:
        0 0 5px rgba(255, 255, 255, 0.8),
        /* 内层光晕 */ 0 0 10px rgba(255, 255, 255, 0.6),
        /* 中层光晕 */ 0 0 15px rgba(255, 255, 255, 0.4),
        /* 外层光晕 */ 0 0 20px rgba(255, 255, 255, 0.2); /* 更大的外层光晕 */
      animation: flicker 1.5s infinite alternate; /* 字体闪烁效果 */
      opacity: 0; /* 初始透明度 */
      animation:
        fadeIn 3s forwards,
        flicker 1.5s infinite alternate;
    }

    p {
      margin: unset;
      flex-grow: 1;
      font-size: 20px;
    }

    em {
      color: var(--color-accent);
      position: relative;
    }
  }
}
</style>
