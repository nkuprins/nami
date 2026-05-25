import {createRouter, createWebHistory} from "vue-router";
import HomeView from "./views/HomeView.vue";
import NotFoundView from "./views/NotFoundView.vue";

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        {path: '/', name: 'home', component: HomeView},
        {path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView}
    ],
});