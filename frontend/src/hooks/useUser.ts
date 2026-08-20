import { useQuery } from '@tanstack/react-query'
import { useApi } from './useApi'
import { endpoints } from '../config/api'
import type { User } from '@/types'

export function useCurrentUser() {
  const { get, accessToken } = useApi()

  return useQuery({
    queryKey: ['current-user'],
    enabled: !!accessToken,
    retry: false,
    refetchOnWindowFocus: false,
    queryFn: async (): Promise<User | null> => {
      try {
        const response = await get<User>(endpoints.users.me)
        if (!response.success) return null
        return response.data ?? null
      } catch {
        return null
      }
    },
  })
}
