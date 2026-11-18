let activeFamilyId: string | null = null
let activeMemberId: string | null = null
let activeUserId: string | null = null

export function setFamilyRuntimeContext(input: {
  familyId: string | null
  memberId?: string | null
  userId?: string | null
}) {
  activeFamilyId = input.familyId
  if (input.memberId !== undefined) activeMemberId = input.memberId
  if (input.userId !== undefined) activeUserId = input.userId
}

export function getActiveFamilyId(): string | null {
  return activeFamilyId
}

export function getActiveMemberId(): string | null {
  return activeMemberId
}

export function getActiveUserId(): string | null {
  return activeUserId
}

export function requireActiveFamilyId(): string {
  if (!activeFamilyId) {
    throw new Error('Chưa chọn gia đình — đợi FamilyContext tải hoặc tạo gia đình.')
  }
  return activeFamilyId
}