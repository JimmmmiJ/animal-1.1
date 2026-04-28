import { ref } from 'vue'

export function useSaveAction() {
  const savingAction = ref('')

  async function runSaveAction(actionKey, action) {
    if (savingAction.value) return undefined
    savingAction.value = actionKey
    try {
      return await action()
    } finally {
      savingAction.value = ''
    }
  }

  return {
    savingAction,
    runSaveAction
  }
}
