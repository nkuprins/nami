<script setup lang="ts">
import {computed, onUnmounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {
  addProperty,
  requestPresignedUrls,
  uploadFilesToS3,
} from '../api/properties';
import type {
  Feature,
  PropertyKind,
  PropertyType,
} from '../types/propertyItem';
import LocationPopover from '../components/hero/LocationPopover.vue';
import type {Location} from '../data/rawLocations';

const router = useRouter();

const submitting = ref(false);
const submitError = ref('');
const touched = ref(false);

// Form state
const type = ref<PropertyType>('buy');
const propertyKind = ref<PropertyKind>('apartment');
const title = ref('');
const description = ref('');
const price = ref('');
const selectedLocation = ref<Location | null>(null);
const address = ref('');
const rooms = ref('');
const m2 = ref('');
const landM2 = ref('');
const floor = ref('');
const totalFloors = ref('');
const yearBuilt = ref('');
const completion = ref<'ready' | 'not_ready' | ''>('');
const features = ref<Feature[]>([]);
const photoFiles = ref<File[]>([]);
const photoPreviews = ref<string[]>([]);
const fileInputRef = ref<HTMLInputElement | null>(null);

const isOpenDistrict = ref(false);

const selectedDistrictName = computed(() =>
    selectedLocation.value
        ? `${selectedLocation.value.district}, ${selectedLocation.value.city}`
        : ''
);

const FEATURE_OPTIONS: { value: Feature; label: string }[] = [
  {value: 'balcony', label: 'Balcony'},
  {value: 'parking', label: 'Parking'},
  {value: 'elevator', label: 'Elevator'},
  {value: 'furnished', label: 'Furnished'},
  {value: 'pets', label: 'Pets allowed'},
  {value: 'new_building', label: 'New building'},
];

function onFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  for (const file of Array.from(input.files ?? [])) {
    if (photoFiles.value.some((f) => f.name === file.name)) continue;
    photoFiles.value.push(file);
    photoPreviews.value.push(URL.createObjectURL(file));
  }
  input.value = '';
}

function removePhoto(i: number) {
  URL.revokeObjectURL(photoPreviews.value[i]);
  photoFiles.value.splice(i, 1);
  photoPreviews.value.splice(i, 1);
}

onUnmounted(() => {
  photoPreviews.value.forEach((url) => URL.revokeObjectURL(url));
});

function numericInput(e: InputEvent) {
  if (e.data !== null && !/^\d+$/.test(e.data)) e.preventDefault();
}

function toggleFeature(f: Feature) {
  const i = features.value.indexOf(f);
  if (i === -1) features.value.push(f);
  else features.value.splice(i, 1);
}

// Validation
const errors = computed(() => {
  const e: Record<string, string> = {};
  if (!title.value.trim()) e.title = 'Required';
  if (!price.value || isNaN(Number(price.value)) || Number(price.value) <= 0)
    e.price = 'Enter a valid price';
  if (!selectedLocation.value) e.district = 'Required';
  if (!address.value.trim()) e.address = 'Required';
  if (!rooms.value || isNaN(Number(rooms.value)) || Number(rooms.value) < 1)
    e.rooms = 'Enter number of rooms';
  if (!m2.value || isNaN(Number(m2.value)) || Number(m2.value) <= 0)
    e.m2 = 'Enter area in m²';
  if (photoFiles.value.length === 0) e.photos = 'At least one photo required';
  if (type.value === 'new_project' && !completion.value)
    e.completion = 'Required for new projects';
  return e;
});

const isValid = computed(() => Object.keys(errors.value).length === 0);

function fieldError(field: string): string {
  return touched.value ? (errors.value[field] ?? '') : '';
}

async function submit() {
  touched.value = true;
  if (!isValid.value) return;
  submitting.value = true;
  submitError.value = '';
  try {
    const slots = await requestPresignedUrls(
        photoFiles.value.map((f) => f.name)
    );
    const photoList = await uploadFilesToS3(photoFiles.value, slots);
    const item = await addProperty({
      type: type.value,
      propertyKind: propertyKind.value,
      title: title.value.trim(),
      description: description.value.trim(),
      price: Number(price.value),
      rooms: Number(rooms.value),
      m2: Number(m2.value),
      landM2:
          propertyKind.value === 'house' && landM2.value
              ? Number(landM2.value)
              : undefined,
      floor: floor.value ? Number(floor.value) : undefined,
      totalFloors: totalFloors.value ? Number(totalFloors.value) : undefined,
      yearBuilt: yearBuilt.value ? Number(yearBuilt.value) : undefined,
      features: features.value,
      district: selectedLocation.value!.district,
      city: selectedLocation.value!.city,
      address: address.value.trim(),
      coords: {lat: 56.946, lng: 24.105},
      photos: photoList,
      completion:
          type.value === 'new_project' && completion.value
              ? completion.value
              : undefined,
    });
    await router.push(`/property/${item.id}`);
  } catch {
    submitError.value = 'Something went wrong. Please try again.';
    submitting.value = false;
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 sm:px-6 py-10 sm:py-14">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-ink">Add a property</h1>
      <p class="text-sm text-ink-3 mt-1">
        Fill in the details below to list your property.
      </p>
    </div>

    <form class="flex flex-col gap-10" @submit.prevent="submit">
      <section class="flex flex-col gap-4">
        <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
          Listing type
        </h2>

        <div class="flex flex-col gap-3">
          <div class="flex flex-col gap-1.5">
            <p class="text-sm font-medium text-ink">Transaction type</p>
            <div class="flex gap-2 flex-wrap">
              <button
                  v-for="opt in [
                  { id: 'buy', label: 'For sale' },
                  { id: 'rent', label: 'For rent' },
                  { id: 'new_project', label: 'New project' },
                ]"
                  :key="opt.id"
                  type="button"
                  class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
                  :class="
                  type === opt.id
                    ? 'bg-ink text-bg border-ink'
                    : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
                "
                  @click="type = opt.id as PropertyType"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <div class="flex flex-col gap-1.5">
            <p class="text-sm font-medium text-ink">Property kind</p>
            <div class="flex gap-2">
              <button
                  v-for="opt in [
                  { id: 'apartment', label: 'Apartment' },
                  { id: 'house', label: 'House' },
                ]"
                  :key="opt.id"
                  type="button"
                  class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
                  :class="
                  propertyKind === opt.id
                    ? 'bg-ink text-bg border-ink'
                    : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
                "
                  @click="propertyKind = opt.id as PropertyKind"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="flex flex-col gap-4">
        <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
          Basic info
        </h2>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-title">
            Title <span class="text-red-500">*</span>
          </label>
          <input
              id="ap-title"
              v-model="title"
              type="text"
              placeholder="e.g. Bright 3-room apartment in Centrs"
              class="h-10 px-3 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
              :class="
              fieldError('title')
                ? 'border-red-400 bg-red-50'
                : 'border-line bg-bg'
            "
          />
          <p v-if="fieldError('title')" class="text-xs text-red-500">
            {{ fieldError('title') }}
          </p>
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-description"
          >Description</label
          >
          <textarea
              id="ap-description"
              v-model="description"
              rows="4"
              placeholder="Describe the property…"
              class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-none focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
        </div>
      </section>

      <section class="flex flex-col gap-4">
        <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
          Pricing
        </h2>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-price">
            Price (€) <span class="text-red-500">*</span>
          </label>
          <input
              id="ap-price"
              v-model="price"
              type="text"
              inputmode="numeric"
              placeholder="e.g. 185000"
              @beforeinput="numericInput"
              class="h-10 px-3 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
              :class="
              fieldError('price')
                ? 'border-red-400 bg-red-50'
                : 'border-line bg-bg'
            "
          />
          <p v-if="fieldError('price')" class="text-xs text-red-500">
            {{ fieldError('price') }}
          </p>
        </div>

        <div v-if="type === 'new_project'" class="flex flex-col gap-1.5">
          <p class="text-sm font-medium text-ink">
            Completion status <span class="text-red-500">*</span>
          </p>
          <div class="flex gap-2">
            <button
                v-for="opt in [
                { id: 'ready', label: 'Ready' },
                { id: 'not_ready', label: 'Under construction' },
              ]"
                :key="opt.id"
                type="button"
                class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
                :class="
                completion === opt.id
                  ? 'bg-ink text-bg border-ink'
                  : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
              "
                @click="completion = opt.id as 'ready' | 'not_ready'"
            >
              {{ opt.label }}
            </button>
          </div>
          <p v-if="fieldError('completion')" class="text-xs text-red-500">
            {{ fieldError('completion') }}
          </p>
        </div>
      </section>

      <section class="flex flex-col gap-4">
        <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
          Location
        </h2>

        <div class="flex flex-col gap-1.5 relative">
          <label class="text-sm font-medium text-ink" for="ap-district-toggle">
            District <span class="text-red-500">*</span>
          </label>

          <button
              id="ap-district-toggle"
              type="button"
              class="h-10 px-3 rounded-lg border text-sm text-ink bg-bg flex items-center justify-between focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-all text-left"
              :class="
              fieldError('district')
                ? 'border-red-400 bg-red-50'
                : 'border-line'
            "
              @click="isOpenDistrict = !isOpenDistrict"
          >
            <span v-if="selectedLocation" class="text-ink font-medium">
              {{ selectedDistrictName }}
            </span>
            <span v-else class="text-ink-3">Select a district…</span>
            <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 20 20"
                fill="currentColor"
                class="size-4 text-ink-3 transition-transform duration-200"
                :class="{ 'rotate-180': isOpenDistrict }"
            >
              <path
                  fill-rule="evenodd"
                  d="M5.22 8.22a.75.75 0 0 1 1.06 0L10 11.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 9.28a.75.75 0 0 1 0-1.06Z"
                  clip-rule="evenodd"
              />
            </svg>
          </button>

          <div
              v-if="isOpenDistrict"
              class="fixed inset-0 z-40"
              @click="isOpenDistrict = false"
          ></div>

          <div
              v-if="isOpenDistrict"
              class="absolute top-[calc(100%+4px)] left-0 z-50 w-full bg-bg border border-line rounded-lg shadow-xl p-3"
          >
            <LocationPopover
                :model-value="selectedLocation ? [selectedLocation] : []"
                :multiple="false"
                @update:model-value="
                (locs) => {
                  selectedLocation = locs[0] ?? null;
                  isOpenDistrict = false;
                }
              "
            />
          </div>

          <p v-if="fieldError('district')" class="text-xs text-red-500 mt-1">
            {{ fieldError('district') }}
          </p>
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-address">
            Street address <span class="text-red-500">*</span>
          </label>
          <input
              id="ap-address"
              v-model="address"
              type="text"
              placeholder="e.g. Brīvības iela 12, apt. 4"
              class="h-10 px-3 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
              :class="
              fieldError('address')
                ? 'border-red-400 bg-red-50'
                : 'border-line bg-bg'
            "
          />
          <p v-if="fieldError('address')" class="text-xs text-red-500">
            {{ fieldError('address') }}
          </p>
        </div>
      </section>

      <section class="flex flex-col gap-4">
        <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
          Details
        </h2>

        <div class="grid grid-cols-2 gap-4">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-ink" for="ap-rooms">
              Rooms <span class="text-red-500">*</span>
            </label>
            <input
                id="ap-rooms"
                v-model="rooms"
                type="text"
                inputmode="numeric"
                placeholder="e.g. 3"
                @beforeinput="numericInput"
                class="h-10 px-3 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
                :class="
                fieldError('rooms')
                  ? 'border-red-400 bg-red-50'
                  : 'border-line bg-bg'
              "
            />
            <p v-if="fieldError('rooms')" class="text-xs text-red-500">
              {{ fieldError('rooms') }}
            </p>
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-ink" for="ap-m2">
              Area (m²) <span class="text-red-500">*</span>
            </label>
            <input
                id="ap-m2"
                v-model="m2"
                type="text"
                inputmode="numeric"
                placeholder="e.g. 72"
                @beforeinput="numericInput"
                class="h-10 px-3 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
                :class="
                fieldError('m2')
                  ? 'border-red-400 bg-red-50'
                  : 'border-line bg-bg'
              "
            />
            <p v-if="fieldError('m2')" class="text-xs text-red-500">
              {{ fieldError('m2') }}
            </p>
          </div>
        </div>

        <div v-if="propertyKind === 'house'" class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-land"
          >Land area (m²)</label
          >
          <input
              id="ap-land"
              v-model="landM2"
              type="text"
              inputmode="numeric"
              placeholder="e.g. 600"
              @beforeinput="numericInput"
              class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-ink" for="ap-floor"
            >Floor</label
            >
            <input
                id="ap-floor"
                v-model="floor"
                type="text"
                inputmode="numeric"
                placeholder="e.g. 4"
                @beforeinput="numericInput"
                class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-ink" for="ap-total-floors"
            >Total floors</label
            >
            <input
                id="ap-total-floors"
                v-model="totalFloors"
                type="text"
                inputmode="numeric"
                placeholder="e.g. 9"
                @beforeinput="numericInput"
                class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
            />
          </div>
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="ap-year"
          >Year built</label
          >
          <input
              id="ap-year"
              v-model="yearBuilt"
              type="text"
              inputmode="numeric"
              placeholder="e.g. 2018"
              @beforeinput="numericInput"
              class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
        </div>
      </section>

      <section class="flex flex-col gap-4">
        <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
          Features
        </h2>
        <div class="flex flex-wrap gap-2">
          <button
              v-for="opt in FEATURE_OPTIONS"
              :key="opt.value"
              type="button"
              class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
              :class="
              features.includes(opt.value)
                ? 'bg-ink text-bg border-ink'
                : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
            "
              @click="toggleFeature(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section class="flex flex-col gap-4">
        <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
          Photos
        </h2>
        <div class="flex flex-col gap-1.5">
          <p class="text-sm font-medium text-ink">
            Photos <span class="text-red-500">*</span>
          </p>
          <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              multiple
              class="hidden"
              @change="onFilesSelected"
          />
          <button
              type="button"
              class="self-start h-9 px-4 rounded-full text-sm font-medium border transition-colors"
              :class="
              fieldError('photos')
                ? 'border-red-400 text-red-500'
                : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
            "
              @click="fileInputRef?.click()"
          >
            + Add photos
          </button>
          <div v-if="photoPreviews.length" class="grid grid-cols-3 gap-2 mt-1">
            <div v-for="(src, i) in photoPreviews" :key="i" class="relative">
              <img
                  :src="src"
                  class="w-full aspect-square object-cover rounded-lg"
                  :alt="`Photo ${i + 1}`"
              />
              <button
                  type="button"
                  class="absolute top-1 right-1 size-5 rounded-full bg-ink/70 text-bg text-xs flex items-center justify-center hover:bg-ink transition-colors"
                  @click="removePhoto(i)"
              >
                ✕
              </button>
            </div>
          </div>
          <p v-if="fieldError('photos')" class="text-xs text-red-500">
            {{ fieldError('photos') }}
          </p>
        </div>
      </section>

      <div class="flex items-center gap-4 pt-2">
        <button
            type="submit"
            :disabled="submitting"
            class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ submitting ? 'Submitting…' : 'Publish listing' }}
        </button>
        <RouterLink
            to="/"
            class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors"
        >
          Cancel
        </RouterLink>
      </div>

      <p v-if="submitError" class="text-sm text-red-500">{{ submitError }}</p>
    </form>
  </div>
</template>
