export type UserRow = {
  id: string;
  phone_e164?: string;
  name: string;
  age: number;
  bio: string | null;
  job_title: string | null;
  company: string | null;
  height: string | null;
  interests: string[] | null;
  is_verified: boolean;
  profile_completed?: boolean;
};

export type PromptRow = {
  id: string;
  question: string;
  answer: string;
  position: number;
};

export type PhotoRow = {
  id: string;
  storage_key: string;
  cdn_url: string;
  position: number;
};

export function serializeProfile(
  user: UserRow,
  prompts: PromptRow[] = [],
  photos: PhotoRow[] = []
) {
  return {
    id: user.id,
    phone: user.phone_e164,
    name: user.name,
    age: user.age,
    bio: user.bio ?? "",
    jobTitle: user.job_title ?? "",
    company: user.company ?? "",
    height: user.height ?? "",
    interests: user.interests ?? [],
    isVerified: user.is_verified,
    profileCompleted: user.profile_completed ?? true,
    prompts: prompts
      .sort((lhs, rhs) => lhs.position - rhs.position)
      .map((prompt) => ({
        id: prompt.id,
        question: prompt.question,
        answer: prompt.answer
      })),
    photos: photos
      .sort((lhs, rhs) => lhs.position - rhs.position)
      .map((photo) => ({
        id: photo.id,
        storageKey: photo.storage_key,
        cdnUrl: photo.cdn_url,
        position: photo.position
      }))
  };
}

export function serializeCompactProfile(
  user: UserRow & { photo_url?: string | null; distance?: number | null; has_super_liked_me?: boolean | null }
) {
  return {
    id: user.id,
    name: user.name,
    age: user.age,
    bio: user.bio ?? "",
    jobTitle: user.job_title ?? "",
    company: user.company ?? "",
    height: user.height ?? "",
    interests: user.interests ?? [],
    isVerified: user.is_verified,
    photoUrl: user.photo_url ?? null,
    distance: user.distance ?? 0,
    hasSuperLikedMe: user.has_super_liked_me ?? false
  };
}
