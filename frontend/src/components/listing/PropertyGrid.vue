<script setup lang="ts">
import {mockListings} from "../../mock/listings";
import PropertyItem from "./PropertyItem.vue";
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 lg:gap-8">
    <PropertyItem :property="mockListings[0]"/>
    <PropertyItem :property="mockListings[1]"/>
  </div>
</template>