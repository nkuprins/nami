import {createRouter, createWebHistory} from "vue-router";
import HomeView from "./views/HomeView.vue";
import NotFoundView from "./views/NotFoundView.vue";
import ListingView from "./views/ListingView.vue";
import AddPropertyView from "./views/AddPropertyView.vue";
import VerifyEmailView from "./views/VerifyEmailView.vue";
import ResetPasswordView from "./views/ResetPasswordView.vue";
import {useAuth} from "./composables/useAuth";

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        {path: '/', name: 'home', component: HomeView},
        {path: '/property/:id', name: 'property', component: ListingView, props: true},
        {path: '/add-property', name: 'add-property', component: AddPropertyView},
        {path: '/verify-email', name: 'verify-email', component: VerifyEmailView},
        {path: '/reset-password', name: 'reset-password', component: ResetPasswordView},
        {path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView}
    ],
});

router.beforeEach((to) => {
    const {isAuthenticated} = useAuth();
    if (to.name === 'add-property' && !isAuthenticated.value) {
        return {name: 'home'};
    }
});
