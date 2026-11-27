<template>
  <div class="admin-users-page py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center mb-4 animate-fade-in-up">
        <div>
          <h1 class="display-5 mb-1 glass-text">User Management</h1>
          <p class="lead glass-subtitle mb-0">View, create, edit, and manage all system users (including inactive accounts).</p>
        </div>
        <router-link :to="{ name: 'admin-user-create' }" class="btn btn-primary interactive-lift">
          <UserPlus class="me-2" />Add New User
        </router-link>
      </div>

      <!-- Modals -->
      <LoadingModal :visible="isLoading"/>
      <ErrorModal :message="error" :visible="!!error" title="An Error Occurred"
                  @close="error = ''"/>
      <SuccessModal :message="successMessage" :visible="!!successMessage" title="Success"
                    @close="successMessage = ''"/>
      <!-- Soft delete: reversible, so it is deliberately distinct from hard delete. -->
      <ConfirmModal
        ref="confirmModalRef"
        modal-id="deactivate-user-modal"
        :message="`Deactivate user '${userToDelete?.firstName} ${userToDelete?.lastName}' (ID: ${userToDelete?.id})? The account will be marked Inactive and can be restored later.`"
        title="Confirm Deactivation"
        type="warning"
        confirm-text="Deactivate"
        @close="userToDelete = null"
        @confirm="handleDelete"
      />
      <!-- Hard delete: permanent and irreversible — the wording must be explicit. -->
      <ConfirmModal
        ref="hardDeleteModalRef"
        modal-id="hard-delete-user-modal"
        :message="`PERMANENTLY DELETE user '${userToHardDelete?.firstName} ${userToHardDelete?.lastName}' (ID: ${userToHardDelete?.id})? This is a PERMANENT action and cannot be undone. All data belonging to this user will be removed and cannot be recovered.`"
        title="Permanently Delete User"
        type="danger"
        confirm-text="Delete Permanently"
        @close="userToHardDelete = null"
        @confirm="handleHardDelete"
      />

      <!-- User List Table -->
      <div v-if="!isLoading && users.length > 0" class="card glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover glass-table mb-0">
              <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Slug</th>
                <th>Roles</th>
                <th class="text-center">Status</th>
                <th class="text-end">Actions</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.firstName }} {{ user.lastName }}</td>
                <td>{{ user.email }}</td>
                <td>
                  <a :href="'/' + user.slug" class="text-info text-decoration-none hover-underline" target="_blank">
                    /{{ user.slug }}
                  </a>
                </td>
                <td>
                    <span v-for="role in user.roles" :key="role" class="badge bg-primary me-1">
                      {{ role.replace('ROLE_', '') }}
                    </span>
                </td>
                <td class="text-center">
                    <span :class="['badge', user.active ? 'bg-success' : 'bg-danger']">
                      {{ user.active ? 'Active' : 'Inactive' }}
                    </span>
                </td>
                <td class="text-end">
                  <!-- Compact icon-only actions: one horizontal, nowrap group keeps the
                       table readable at narrow widths. Tooltips (title + aria-label)
                       carry the meaning that the removed inline labels used to show. -->
                  <div aria-label="User Actions" class="btn-group btn-group-sm user-actions" role="group">
                    <a :href="'/' + user.slug" class="btn btn-sm btn-outline-info row-action" target="_blank"
                       title="View Public Profile" aria-label="View Public Profile" data-testid="view-public-profile">
                      <Eye />
                    </a>
                    <button type="button" class="btn btn-sm btn-outline-primary row-action" title="Edit User"
                            aria-label="Edit User" :disabled="isActionInFlight" data-testid="edit-user"
                            @click="editUser(user.id)">
                      <Pencil />
                    </button>
                    <button v-if="user.active === false" type="button"
                            class="btn btn-sm btn-outline-success row-action" title="Restore User"
                            aria-label="Restore User" :disabled="isActionInFlight" data-testid="restore-user"
                            @click="restoreAccount(user)">
                      <RotateCcw />
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-danger row-action"
                            title="Deactivate User (Soft Delete)" aria-label="Deactivate User (Soft Delete)"
                            :disabled="user.active === false || isActionInFlight" data-testid="deactivate-user"
                            @click="confirmDeactivate(user)">
                      <UserX />
                    </button>
                    <button type="button" class="btn btn-sm btn-danger row-action"
                            title="Permanently Delete User (Hard Delete)"
                            aria-label="Permanently Delete User (Hard Delete)" :disabled="isActionInFlight"
                            data-testid="hard-delete-user" @click="confirmHardDelete(user)">
                      <Trash2 />
                    </button>
                  </div>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
        <!-- Pagination Footer -->
        <div class="card-footer glass-card-footer d-flex justify-content-between align-items-center py-3">
          <div class="text-muted small">
            Showing {{ users.length }} of {{ totalElements }} users
          </div>
          <nav aria-label="User pagination">
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: currentPage === 0 }">
                <button class="page-link" :disabled="isActionInFlight" @click="changePage(currentPage - 1)">Previous</button>
              </li>
              <li v-for="p in totalPages" :key="p" class="page-item" :class="{ active: currentPage === p - 1 }">
                <button class="page-link" :disabled="isActionInFlight" @click="changePage(p - 1)">{{ p }}</button>
              </li>
              <li class="page-item" :class="{ disabled: currentPage === totalPages - 1 }">
                <button class="page-link" :disabled="isActionInFlight" @click="changePage(currentPage + 1)">Next</button>
              </li>
            </ul>
          </nav>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!isLoading" class="text-center p-5 glass-card animate-fade-in-up"
           style="animation-delay: 0.1s;">
        <div class="empty-state-icon mb-4">
          <Users />
        </div>
        <h4 class="glass-title">No Users Found</h4>
        <p class="glass-subtitle">There are currently no other users in the system.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { UserPlus, Eye, Pencil, RotateCcw, Trash2, UserX, Users } from 'lucide-vue-next';
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {
  deleteAdminUser,
  getAdminUsers,
  hardDeleteUser,
  restoreUser as restoreAdminUser
} from '@/services/api/admin.api';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ConfirmModal from '@/components/common/modals/ConfirmModal.vue';

const router = useRouter();

import type { User } from '@/types';

const users = ref<User[]>([]);
const isLoading = ref(true);
const isActionInFlight = ref(false);
const error = ref<string>('');
const successMessage = ref<string>('');
const userToDelete = ref<User | null>(null);
const userToHardDelete = ref<User | null>(null);
const confirmModalRef = ref<{ show: () => void } | null>(null);
const hardDeleteModalRef = ref<{ show: () => void } | null>(null);

// Pagination State
const currentPage = ref(0);
const pageSize = ref(10);
const totalPages = ref(0);
const totalElements = ref(0);


const fetchUsers = async () => {
  try {
    isLoading.value = true;
    error.value = '';

    const response = await getAdminUsers({
      page: currentPage.value,
      size: pageSize.value
    });

    if (response && Array.isArray(response.content)) {
      // Intentionally include inactive (soft-deleted) accounts so they can be
      // found and restored. The Inactive badge marks them in the table.
      users.value = response.content;
      totalPages.value = response.totalPages || 0;
      totalElements.value = response.totalElements || 0;
    }
  } catch (err: unknown) {
    console.error("Failed to fetch users:", err);
    error.value = (err as Error).message || 'An unexpected error occurred while fetching users.';
  } finally {
    isLoading.value = false;
  }
};

onMounted(fetchUsers);

const changePage = (page: number) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
    fetchUsers();
  }
};

const editUser = (userId: string | number) => {
  console.log(`[AdminUsersPage] Edit button clicked. Attempting to navigate with user ID: ${userId} (Type: ${typeof userId})`);
  if (!userId && userId !== 0) {
    console.error("[AdminUsersPage] NAVIGATION BLOCKED: The provided user ID is invalid.", userId);
    error.value = "Could not open the edit page because the user's ID is missing. Please refresh and try again.";
    return;
  }
  router.push({name: 'admin-user-edit', params: {id: String(userId)}});
};

const confirmDeactivate = (user: User) => {
  userToDelete.value = user;
  confirmModalRef.value?.show();
};

const handleDelete = async () => {
  const targetUser = userToDelete.value;
  if (!targetUser || isActionInFlight.value) return;

  isActionInFlight.value = true;
  error.value = '';
  successMessage.value = '';
  try {
    await deleteAdminUser(String(targetUser.id));
    successMessage.value = `User '${targetUser.firstName} ${targetUser.lastName}' has been deactivated.`;
    await fetchUsers();
  } catch (err: unknown) {
    console.error("Failed to deactivate user:", err);
    error.value = (err as Error).message || 'An error occurred during deactivation.';
  } finally {
    isActionInFlight.value = false;
  }
};

const restoreAccount = async (user: User) => {
  if (isActionInFlight.value) return;

  isActionInFlight.value = true;
  error.value = '';
  successMessage.value = '';
  try {
    await restoreAdminUser(String(user.id));
    successMessage.value = `User '${user.firstName} ${user.lastName}' has been restored.`;
    await fetchUsers();
  } catch (err: unknown) {
    console.error("Failed to restore user:", err);
    error.value = (err as Error).message || 'An error occurred during restoration.';
  } finally {
    isActionInFlight.value = false;
  }
};

const confirmHardDelete = (user: User) => {
  if (isActionInFlight.value) return;
  userToHardDelete.value = user;
  hardDeleteModalRef.value?.show();
};

const handleHardDelete = async () => {
  const targetUser = userToHardDelete.value;
  if (!targetUser || isActionInFlight.value) return;

  isActionInFlight.value = true;
  error.value = '';
  successMessage.value = '';
  try {
    await hardDeleteUser(String(targetUser.id));
    successMessage.value = `User '${targetUser.firstName} ${targetUser.lastName}' has been permanently deleted.`;
    await fetchUsers();
  } catch (err: unknown) {
    console.error("Failed to permanently delete user:", err);
    error.value = (err as Error).message || 'An error occurred during permanent deletion.';
  } finally {
    isActionInFlight.value = false;
  }
};
</script>

<style scoped>
.display-5 {
  font-weight: 300;
}

.glass-table {
  color: var(--glass-text);
  --bs-table-hover-color: var(--glass-text);
  --bs-table-hover-bg: var(--glass-bg-hover);
}

.glass-table thead th {
  background-color: rgba(var(--bs-body-color-rgb), 0.05);
  border-bottom: 2px solid var(--glass-border-hover);
  color: var(--glass-text);
  font-weight: 500;
}

.glass-table td, .glass-table th {
  border-color: var(--glass-border);
  vertical-align: middle;
}

.badge {
  text-transform: capitalize;
}

/* Row actions: small square icon buttons in a single non-wrapping horizontal
   group. The explicit sizing also prevents the global mobile `.btn` rule from
   turning them back into tall blocks. */
.user-actions {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap;
}

.user-actions > .row-action {
  width: 28px;
  height: 28px;
  min-height: 28px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.user-actions > .row-action svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.empty-state-icon {
  font-size: 4rem;
  color: var(--glass-text);
}

/* Responsive styles for mobile-first design */
@media (max-width: 576px) {
  .admin-users-page {
    padding: 1rem 0;
  }

  .admin-users-page .container {
    padding: 0 1rem;
  }

  .admin-users-page .d-flex {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .admin-users-page .display-5 {
    font-size: 1.8rem;
    text-align: center;
  }

  .admin-users-page .lead {
    font-size: 1rem;
    text-align: center;
  }

  .admin-users-page .btn {
    min-height: 44px;
    font-size: 0.9rem;
  }

  .admin-users-page .user-actions > .row-action {
    min-height: 28px;
    height: 28px;
  }

  .admin-users-page .table-responsive {
    font-size: 0.85rem;
  }

  .admin-users-page th, .admin-users-page td {
    padding: 0.5rem;
  }
}

/* Tablet enhancements */
@media (min-width: 768px) {
  .admin-users-page .card-body {
    padding: 1.5rem;
  }
}

/* Desktop enhancements */
@media (min-width: 1024px) {
  .admin-users-page .container {
    max-width: 1400px;
  }
}
</style>
