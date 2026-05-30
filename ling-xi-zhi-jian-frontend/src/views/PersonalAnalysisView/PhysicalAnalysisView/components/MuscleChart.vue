<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'

interface RegionData {
  name: string
  value: number
  part: string
  color: string
}

const props = defineProps<{
  modelValue?: string | null,
  chartType?: 'muscle' | 'fat',
  muscleData?: RegionData[],
  fatData?: RegionData[]
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const hoveredRegion = ref<string | null>(null)

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let humanGroup: THREE.Group
let animationId: number

const muscleRegions = {
  head: { name: '头部肌肉', value: 1.2, color: '#1890ff' },
  trapeziusL: { name: '左斜方肌', value: 0.8, color: '#722ed1' },
  trapeziusR: { name: '右斜方肌', value: 0.8, color: '#722ed1' },
  deltoidL: { name: '左三角肌', value: 0.6, color: '#1890ff' },
  deltoidR: { name: '右三角肌', value: 0.6, color: '#1890ff' },
  pectoralL: { name: '左胸肌', value: 1.5, color: '#1890ff' },
  pectoralR: { name: '右胸肌', value: 1.5, color: '#1890ff' },
  latL: { name: '左背阔肌', value: 1.3, color: '#722ed1' },
  latR: { name: '右背阔肌', value: 1.3, color: '#722ed1' },
  absUp: { name: '上腹肌', value: 0.8, color: '#52c41a' },
  absLow: { name: '下腹肌', value: 0.9, color: '#52c41a' },
  obliqueL: { name: '左腹外斜肌', value: 0.5, color: '#fa8c16' },
  obliqueR: { name: '右腹外斜肌', value: 0.5, color: '#fa8c16' },
  bicepL: { name: '左肱二头肌', value: 0.4, color: '#1890ff' },
  bicepR: { name: '右肱二头肌', value: 0.4, color: '#1890ff' },
  tricepL: { name: '左肱三头肌', value: 0.35, color: '#722ed1' },
  tricepR: { name: '右肱三头肌', value: 0.35, color: '#722ed1' },
  forearmL: { name: '左前臂肌', value: 0.5, color: '#1890ff' },
  forearmR: { name: '右前臂肌', value: 0.5, color: '#1890ff' },
  quadL: { name: '左股四头肌', value: 2.0, color: '#1890ff' },
  quadR: { name: '右股四头肌', value: 2.0, color: '#1890ff' },
  hamL: { name: '左腘绳肌', value: 1.2, color: '#722ed1' },
  hamR: { name: '右腘绳肌', value: 1.2, color: '#722ed1' },
  calfL: { name: '左小腿肌', value: 0.8, color: '#1890ff' },
  calfR: { name: '右小腿肌', value: 0.8, color: '#1890ff' },
}

const fatRegions = {
  visceral: { name: '内脏脂肪', value: 8, color: '#fa8c16' },
  subcutaneous: { name: '皮下脂肪', value: 5.2, color: '#ff7b7b' },
  limb: { name: '四肢脂肪', value: 4.0, color: '#ffd77e' }
}

type RegionRecord = Record<string, { name: string; value: number; color: string }>

const regions = computed<RegionRecord>(() => {
  const data = props.chartType === 'fat' ? props.fatData : props.muscleData
  if (data && data.length > 0) {
    const map: RegionRecord = {}
    data.forEach(item => {
      map[item.part] = { name: item.name, value: item.value, color: item.color }
    })
    return map
  }
  return props.chartType === 'fat' ? fatRegions : muscleRegions
})

const initThree = () => {
  if (!containerRef.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf8fafc)

  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.position.set(0, 0, 4)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  containerRef.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.enableZoom = false
  controls.autoRotate = true
  controls.autoRotateSpeed = 1.5

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(5, 5, 5)
  directionalLight.castShadow = true
  scene.add(directionalLight)

  const pointLight = new THREE.PointLight(0x1890ff, 0.5)
  pointLight.position.set(-3, 3, 3)
  scene.add(pointLight)

  humanGroup = new THREE.Group()
  scene.add(humanGroup)

  createHumanModel()
  createCircularGrid()
  animate()
}

const createHumanModel = () => {
  const muscleColors = ['#1890ff', '#722ed1', '#52c41a', '#fa8c16']
  const fatColors = ['#ff7b7b', '#fa8c16', '#ffd77e', '#52c41a']
  const colors = props.chartType === 'fat' ? fatColors : muscleColors

  const createMuscleMaterial = (color: string) => new THREE.MeshPhysicalMaterial({
    color: new THREE.Color(color),
    metalness: 0.05,
    roughness: 0.5,
    clearcoat: 0.4,
    clearcoatRoughness: 0.3,
    transparent: true,
    opacity: 0.92,
  })

  const bodyParts: { name: string, geometry: THREE.BufferGeometry, position: [number, number, number], rotation?: [number, number, number], color: string }[] = []

  const headGeo = new THREE.SphereGeometry(0.16, 32, 32)
  headGeo.scale(0.9, 1.0, 0.85)
  bodyParts.push({ name: 'head', geometry: headGeo, position: [0, 1.08, 0], color: colors[0] })

  const neckGeo = new THREE.CylinderGeometry(0.055, 0.07, 0.14, 20)
  bodyParts.push({ name: 'neck', geometry: neckGeo, position: [0, 0.92, 0], color: colors[0] })

  const trapL = new THREE.SphereGeometry(0.08, 16, 16)
  trapL.scale(1.2, 0.6, 0.8)
  bodyParts.push({ name: 'trapeziusL', geometry: trapL, position: [-0.06, 0.88, -0.02], color: colors[1] })
  bodyParts.push({ name: 'trapeziusR', geometry: trapL.clone(), position: [0.06, 0.88, -0.02], color: colors[1] })

  const deltoidL = new THREE.SphereGeometry(0.07, 20, 20)
  deltoidL.scale(1.0, 1.1, 0.7)
  bodyParts.push({ name: 'deltoidL', geometry: deltoidL, position: [-0.22, 0.75, 0], color: colors[0] })
  bodyParts.push({ name: 'deltoidR', geometry: deltoidL.clone(), position: [0.22, 0.75, 0], color: colors[0] })

  const chestL = new THREE.SphereGeometry(0.12, 20, 20)
  chestL.scale(1.0, 1.3, 0.6)
  bodyParts.push({ name: 'pectoralL', geometry: chestL, position: [-0.1, 0.62, 0.06], color: colors[0] })
  bodyParts.push({ name: 'pectoralR', geometry: chestL.clone(), position: [0.1, 0.62, 0.06], color: colors[0] })

  const backL = new THREE.SphereGeometry(0.11, 20, 20)
  backL.scale(1.0, 1.2, 0.5)
  bodyParts.push({ name: 'latL', geometry: backL, position: [-0.1, 0.58, -0.06], color: colors[1] })
  bodyParts.push({ name: 'latR', geometry: backL.clone(), position: [0.1, 0.58, -0.06], color: colors[1] })

  const absTop = new THREE.SphereGeometry(0.1, 20, 20)
  absTop.scale(1.1, 0.8, 0.5)
  bodyParts.push({ name: 'absUp', geometry: absTop, position: [0, 0.52, 0.07], color: colors[2] })

  const absBot = new THREE.SphereGeometry(0.09, 20, 20)
  absBot.scale(1.0, 0.9, 0.45)
  bodyParts.push({ name: 'absLow', geometry: absBot, position: [0, 0.38, 0.06], color: colors[2] })

  const obliqL = new THREE.SphereGeometry(0.08, 16, 16)
  obliqL.scale(0.8, 1.2, 0.5)
  bodyParts.push({ name: 'obliqueL', geometry: obliqL, position: [-0.14, 0.45, 0.03], color: colors[3] })
  bodyParts.push({ name: 'obliqueR', geometry: obliqL.clone(), position: [0.14, 0.45, 0.03], color: colors[3] })

  const pelvisGeo = new THREE.SphereGeometry(0.13, 20, 20)
  pelvisGeo.scale(1.3, 0.7, 0.7)
  bodyParts.push({ name: 'pelvis', geometry: pelvisGeo, position: [0, 0.28, 0], color: colors[1] })

  const bicepL = new THREE.CapsuleGeometry(0.04, 0.12, 12, 20)
  bodyParts.push({ name: 'bicepL', geometry: bicepL, position: [-0.28, 0.55, 0.02], rotation: [0, 0, 0.4], color: colors[0] })
  bodyParts.push({ name: 'bicepR', geometry: bicepL.clone(), position: [0.28, 0.55, 0.02], rotation: [0, 0, -0.4], color: colors[0] })

  const tricepL = new THREE.CapsuleGeometry(0.035, 0.1, 12, 20)
  bodyParts.push({ name: 'tricepL', geometry: tricepL, position: [-0.3, 0.48, -0.03], rotation: [0, 0, 0.35], color: colors[1] })
  bodyParts.push({ name: 'tricepR', geometry: tricepL.clone(), position: [0.3, 0.48, -0.03], rotation: [0, 0, -0.35], color: colors[1] })

  const forearmL = new THREE.CapsuleGeometry(0.03, 0.16, 12, 20)
  bodyParts.push({ name: 'forearmL', geometry: forearmL, position: [-0.34, 0.28, 0], rotation: [0, 0, 0.2], color: colors[0] })
  bodyParts.push({ name: 'forearmR', geometry: forearmL.clone(), position: [0.34, 0.28, 0], rotation: [0, 0, -0.2], color: colors[0] })

  const handL = new THREE.SphereGeometry(0.035, 16, 16)
  handL.scale(0.8, 1.1, 0.6)
  bodyParts.push({ name: 'handL', geometry: handL, position: [-0.38, 0.12, 0], color: colors[2] })
  bodyParts.push({ name: 'handR', geometry: handL.clone(), position: [0.38, 0.12, 0], color: colors[2] })

  const quadL = new THREE.CapsuleGeometry(0.07, 0.22, 12, 20)
  bodyParts.push({ name: 'quadL', geometry: quadL, position: [-0.1, 0.0, 0.04], color: colors[0] })
  bodyParts.push({ name: 'quadR', geometry: quadL.clone(), position: [0.1, 0.0, 0.04], color: colors[0] })

  const hamL = new THREE.CapsuleGeometry(0.06, 0.18, 12, 20)
  bodyParts.push({ name: 'hamL', geometry: hamL, position: [-0.1, -0.05, -0.04], color: colors[1] })
  bodyParts.push({ name: 'hamR', geometry: hamL.clone(), position: [0.1, -0.05, -0.04], color: colors[1] })

  const calfL = new THREE.CapsuleGeometry(0.045, 0.2, 12, 20)
  bodyParts.push({ name: 'calfL', geometry: calfL, position: [-0.1, -0.42, 0], color: colors[0] })
  bodyParts.push({ name: 'calfR', geometry: calfL.clone(), position: [0.1, -0.42, 0], color: colors[0] })

  const footL = new THREE.BoxGeometry(0.06, 0.04, 0.12)
  bodyParts.push({ name: 'footL', geometry: footL, position: [-0.1, -0.64, 0.03], color: colors[2] })
  bodyParts.push({ name: 'footR', geometry: footL.clone(), position: [0.1, -0.64, 0.03], color: colors[2] })

  bodyParts.forEach((part) => {
    const material = createMuscleMaterial(part.color)
    const mesh = new THREE.Mesh(part.geometry, material)
    mesh.position.set(part.position[0], part.position[1], part.position[2])
    if (part.rotation) {
      mesh.rotation.set(part.rotation[0], part.rotation[1], part.rotation[2])
    }
    mesh.castShadow = true
    mesh.receiveShadow = true
    mesh.userData = { name: part.name, originalColor: part.color, hovered: false }
    humanGroup.add(mesh)
  })

  humanGroup.scale.set(1.3, 1.3, 1.3)
}

const createCircularGrid = () => {
  const gridHelper = new THREE.PolarGridHelper(2, 8, 8, 64, 0xe2e8f0, 0xe2e8f0)
  gridHelper.position.y = -0.8
  scene.add(gridHelper)

  const ringGeometry = new THREE.RingGeometry(0.8, 0.82, 64)
  const ringMaterial = new THREE.MeshBasicMaterial({
    color: 0x1890ff,
    transparent: true,
    opacity: 0.3,
    side: THREE.DoubleSide
  })
  const ring = new THREE.Mesh(ringGeometry, ringMaterial)
  ring.rotation.x = -Math.PI / 2
  ring.position.y = -0.8
  scene.add(ring)

  const glowRingGeometry = new THREE.RingGeometry(0.75, 0.9, 64)
  const glowRingMaterial = new THREE.MeshBasicMaterial({
    color: 0x1890ff,
    transparent: true,
    opacity: 0.1,
    side: THREE.DoubleSide
  })
  const glowRing = new THREE.Mesh(glowRingGeometry, glowRingMaterial)
  glowRing.rotation.x = -Math.PI / 2
  glowRing.position.y = -0.8
  scene.add(glowRing)
}

const animate = () => {
  animationId = requestAnimationFrame(animate)
  
  if (humanGroup) {
    humanGroup.children.forEach((child) => {
      if (child instanceof THREE.Mesh && child.userData.hovered !== undefined) {
        const material = child.material as THREE.MeshPhysicalMaterial
        const time = Date.now() * 0.002
        
        if (child.userData.hovered) {
          material.emissive.setHex(0x1890ff)
          material.emissiveIntensity = 0.3 + Math.sin(time * 2) * 0.1
        } else {
          material.emissive.setHex(0x000000)
          material.emissiveIntensity = 0
        }
      }
    })
  }
  
  controls.update()
  renderer.render(scene, camera)
}

const onMouseMove = (event: MouseEvent) => {
  if (!containerRef.value || !humanGroup) return

  const rect = containerRef.value.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )

  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(mouse, camera)
  
  const intersects = raycaster.intersectObjects(humanGroup.children)
  
  humanGroup.children.forEach(child => {
    if (child instanceof THREE.Mesh) {
      child.userData.hovered = false
    }
  })

  if (intersects.length > 0) {
    const intersected = intersects[0].object
    if (intersected instanceof THREE.Mesh && intersected.userData.name) {
      intersected.userData.hovered = true
      hoveredRegion.value = intersected.userData.name
    }
  } else {
    hoveredRegion.value = null
  }
}

const onResize = () => {
  if (!containerRef.value) return
  
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

onMounted(() => {
  initThree()
  window.addEventListener('resize', onResize)
  containerRef.value?.addEventListener('mousemove', onMouseMove)
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', onResize)
  containerRef.value?.removeEventListener('mousemove', onMouseMove)
  
  if (renderer) {
    renderer.dispose()
    renderer.domElement.remove()
  }
})

watch(() => props.chartType, () => {
  if (humanGroup) {
    scene.remove(humanGroup)
    humanGroup = new THREE.Group()
    scene.add(humanGroup)
    createHumanModel()
  }
})
</script>

<template>
  <div class="muscle-3d-wrapper">
    <div ref="containerRef" class="muscle-3d-container"></div>
    
    <div class="corner-decoration top-left"></div>
    <div class="corner-decoration top-right"></div>
    <div class="corner-decoration bottom-left"></div>
    <div class="corner-decoration bottom-right"></div>
    
    <div class="hover-info" v-if="hoveredRegion && regions[hoveredRegion as keyof typeof regions]">
      <div class="info-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 16v-4M12 8h.01"/>
        </svg>
      </div>
      <div class="info-content">
        <span class="info-label">{{ regions[hoveredRegion as keyof typeof regions]?.name }}</span>
        <span class="info-value">{{ Number(regions[hoveredRegion as keyof typeof regions]?.value || 0).toFixed(1) }} kg</span>
      </div>
    </div>
    
    <div class="control-hint">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 12a9 9 0 11-6.219-8.56"/>
        <path d="M21 3v4h-4"/>
      </svg>
      <span>拖动旋转查看</span>
    </div>
    
    <div class="data-legend">
      <div 
        v-for="(data, key) in regions" 
        :key="key" 
        class="legend-item"
      >
        <div class="legend-dot" :style="{ background: data.color }"></div>
        <span class="legend-name">{{ data.name }}</span>
        <span class="legend-value">{{ Number(data.value).toFixed(1) }}kg</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.muscle-3d-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 320px;
  background: linear-gradient(180deg, #f8fafc 0%, #f0f7ff 100%);
  border-radius: 16px;
  overflow: hidden;
}

.muscle-3d-container {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.corner-decoration {
  position: absolute;
  width: 60px;
  height: 60px;
  pointer-events: none;
  
  &.top-left {
    top: 12px;
    left: 12px;
    border-top: 2px solid rgba(24, 144, 255, 0.2);
    border-left: 2px solid rgba(24, 144, 255, 0.2);
  }
  
  &.top-right {
    top: 12px;
    right: 12px;
    border-top: 2px solid rgba(24, 144, 255, 0.2);
    border-right: 2px solid rgba(24, 144, 255, 0.2);
  }
  
  &.bottom-left {
    bottom: 12px;
    left: 12px;
    border-bottom: 2px solid rgba(24, 144, 255, 0.2);
    border-left: 2px solid rgba(24, 144, 255, 0.2);
  }
  
  &.bottom-right {
    bottom: 12px;
    right: 12px;
    border-bottom: 2px solid rgba(24, 144, 255, 0.2);
    border-right: 2px solid rgba(24, 144, 255, 0.2);
  }
}

.hover-info {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 24px;
  box-shadow: 0 4px 20px rgba(24, 144, 255, 0.15);
  border: 1px solid rgba(24, 144, 255, 0.1);
  animation: fadeIn 0.3s ease;
  
  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateX(-50%) translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateX(-50%) translateY(0);
    }
  }
  
  .info-icon {
    width: 24px;
    height: 24px;
    
    svg {
      width: 100%;
      height: 100%;
      color: #1890ff;
    }
  }
  
  .info-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .info-label {
      font-size: 11px;
      color: #64748b;
    }
    
    .info-value {
      font-size: 14px;
      font-weight: 600;
      color: #1e293b;
    }
  }
}

.control-hint {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
  border-radius: 16px;
  font-size: 11px;
  color: #94a3b8;
  
  svg {
    width: 14px;
    height: 14px;
  }
}

.data-legend {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 16px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  
  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    
    .legend-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    
    .legend-name {
      font-size: 11px;
      color: #64748b;
    }
    
    .legend-value {
      font-size: 11px;
      font-weight: 600;
      color: #1e293b;
    }
  }
}
</style>
