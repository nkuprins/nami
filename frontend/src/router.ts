import {createRouter, createWebHistory} from "vue-router";
import HomeView from "./views/HomeView.vue";
import NotFoundView from "./views/NotFoundView.vue";
import ListingView from "./views/ListingView.vue";
import AddPropertyView from "./views/AddPropertyView.vue";

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        {path: '/', name: 'home', component: HomeView},
        {path: '/property/:id', name: 'property', component: ListingView, props: true},
        {path: '/add-property', name: 'add-property', component: AddPropertyView},
        {path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView}
    ],
});