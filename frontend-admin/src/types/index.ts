export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  profileImageUrl?: string;
  slug: string;
  roles: string[];
  termsAcceptedAt?: string;
  passwordLastChangedAt?: string;
  active?: boolean;
}

export interface UserSkill extends Skill {
  userSkillId: number;
  visible: boolean;
}

export interface Profile {
  headline?: string;
  summary?: string;
  resumeUrl?: string;
  resumeImageUrl?: string;
  linkedinUrl?: string;
  githubUrl?: string;
  websiteUrl?: string;
  publicEmail?: string;
  coverLetterTemplate?: string;
  publiclyVisible: boolean;
  location?: string;
}

export interface Skill {
  skillId: number;
  name: string;
  level: string;
  category?: string;
  iconName?: string;
  icon?: string; // Alias for iconName used in some components
  visible?: boolean;
  description?: string;
  uuid?: string;
}

// Merged into line 14

export interface Project {
  uuid?: string;
  title: string;
  description: string;
  imageUrl?: string;
  mediaUrl?: string;
  liveUrl?: string;
  repoUrl?: string;
  skills: Skill[] | string[];
  /** Phase 7.4 §6: structured key-features bullets, in display order. */
  keyFeatures?: string[];
  visible?: boolean;
  displayOrder?: number;
}

export interface Experience {
  id: number;
  uuid?: string;
  companyName: string;
  companyUrl?: string;
  companyLogoUrl?: string;
  jobTitle: string;
  description?: string;
  achievements?: string;
  location?: string;
  locationType?: string;
  startDate: string;
  endDate?: string;
  isCurrent?: boolean;
  isCurrentJob?: boolean;
  employmentType?: string;
  visible?: boolean;
  displayOrder?: number;
  skills?: (Skill | string)[];
}

export interface Qualification {
  id: number;
  uuid?: string;
  institutionName: string;
  institutionLogoUrl?: string;
  qualificationName: string;
  fieldOfStudy?: string;
  startDate?: string;
  endDate?: string;
  startYear?: number;
  completionYear?: number;
  grade?: string;
  stillStudying?: boolean;
  level?: string;
  institutionWebsite?: string;
  credentialUrl?: string;
  visible?: boolean;
}

export interface Testimonial {
  id: number;
  uuid?: string;
  authorName: string;
  authorTitle?: string;
  quote: string;
  imageUrl?: string;
  visible?: boolean;
}

export interface Portfolio {
  profile?: Profile;
  user?: User;
  projects?: Project[];
  userSkills?: Skill[];
  experiences?: Experience[];
  qualifications?: Qualification[];
  testimonials?: Testimonial[];
}

/**
 * Mirrors the backend `ApplicationStatus` enum (model/enums/ApplicationStatus.java)
 * exactly. TRK-400 regression (2026-11-04): the payload contract keys must be the
 * backend's (`jobTitle`, `jobLocation`) — the frontend previously sent `title` and
 * `location`, which the backend's bean validation rejected with
 * `POST /me/job-applications 400 "Job title cannot be blank"`.
 *
 * Phase 8.4: extended to the full 13-state lifecycle. `INTERVIEWING` and
 * `ACCEPTED` are LEGACY pre-8.4 values retained so existing records (and any
 * persisted rows carrying them) keep rendering correctly.
 */
export type ApplicationStatus =
  | 'INTERESTED'
  | 'PREPARING'
  | 'DRAFT'
  | 'APPLIED'
  | 'ACKNOWLEDGED'
  | 'INTERVIEW_SCHEDULED'
  | 'INTERVIEW_COMPLETED'
  | 'TECHNICAL_ASSESSMENT'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'NO_RESPONSE'
  | 'ARCHIVED'
  | 'INTERVIEWING'
  | 'ACCEPTED';

/** Canonical pipeline order for display + the legacy values at the end. */
export const APPLICATION_STATUS_ORDER: ApplicationStatus[] = [
  'INTERESTED',
  'PREPARING',
  'DRAFT',
  'APPLIED',
  'ACKNOWLEDGED',
  'INTERVIEW_SCHEDULED',
  'INTERVIEW_COMPLETED',
  'TECHNICAL_ASSESSMENT',
  'OFFER',
  'REJECTED',
  'WITHDRAWN',
  'NO_RESPONSE',
  'ARCHIVED',
  'INTERVIEWING',
  'ACCEPTED',
];

/**
 * Display metadata for every status: human label + Bootstrap badge class.
 * Single source of truth for the card, details modal and filters.
 */
export const STATUS_META: Record<ApplicationStatus, { label: string; badge: string; legacy?: boolean }> = {
  INTERESTED: { label: 'Interested', badge: 'bg-info text-dark' },
  PREPARING: { label: 'Preparing', badge: 'bg-info text-dark' },
  DRAFT: { label: 'Draft', badge: 'bg-secondary' },
  APPLIED: { label: 'Applied', badge: 'bg-primary' },
  ACKNOWLEDGED: { label: 'Acknowledged', badge: 'bg-primary' },
  INTERVIEW_SCHEDULED: { label: 'Interview Scheduled', badge: 'bg-warning text-dark' },
  INTERVIEW_COMPLETED: { label: 'Interview Completed', badge: 'bg-warning text-dark' },
  TECHNICAL_ASSESSMENT: { label: 'Technical Assessment', badge: 'bg-warning text-dark' },
  OFFER: { label: 'Offer', badge: 'bg-success' },
  REJECTED: { label: 'Rejected', badge: 'bg-danger' },
  WITHDRAWN: { label: 'Withdrawn', badge: 'bg-dark' },
  NO_RESPONSE: { label: 'No Response', badge: 'bg-secondary' },
  ARCHIVED: { label: 'Archived', badge: 'bg-secondary' },
  INTERVIEWING: { label: 'Interviewing (legacy)', badge: 'bg-warning text-dark', legacy: true },
  ACCEPTED: { label: 'Accepted (legacy)', badge: 'bg-success', legacy: true },
};

export interface JobApplicationNote {
  uuid: string;
  content: string;
  createdAt: string;
}

export interface JobApplication {
  uuid: string;
  jobTitle: string;
  companyName: string;
  jobLocation?: string;
  jobType?: string;
  applicationUrl?: string;
  resumeVersion?: string;
  jobDescription?: string;
  workArrangement?: string;
  discoveredAt?: string;
  deadlineAt?: string;
  followUpAt?: string;
  interviewAt?: string;
  assessmentAt?: string;
  recruiterName?: string;
  recruiterEmail?: string;
  contactDetails?: string;
  salaryRange?: string;
  salaryCurrency?: string;
  salaryPeriod?: string;
  status: ApplicationStatus;
  statusUpdatedAt?: string;
  appliedAt?: string;
  visible?: boolean;
  notes?: JobApplicationNote[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreateJobApplicationRequest {
  jobTitle: string;
  companyName: string;
  jobLocation?: string;
  jobType?: string;
  applicationUrl?: string;
  resumeVersion?: string;
  jobDescription?: string;
  workArrangement?: string;
  discoveredAt?: string;
  deadlineAt?: string;
  followUpAt?: string;
  interviewAt?: string;
  assessmentAt?: string;
  recruiterName?: string;
  recruiterEmail?: string;
  contactDetails?: string;
  salaryRange?: string;
  salaryCurrency?: string;
  salaryPeriod?: string;
  initialNote?: string;
}

export interface UpdateJobApplicationRequest {
  jobTitle?: string;
  companyName?: string;
  jobLocation?: string;
  jobType?: string;
  applicationUrl?: string;
  resumeVersion?: string;
  jobDescription?: string;
  workArrangement?: string;
  discoveredAt?: string;
  deadlineAt?: string;
  followUpAt?: string;
  interviewAt?: string;
  assessmentAt?: string;
  recruiterName?: string;
  recruiterEmail?: string;
  contactDetails?: string;
  salaryRange?: string;
  salaryCurrency?: string;
  salaryPeriod?: string;
  status?: ApplicationStatus;
  appliedAt?: string;
  visible?: boolean;
}

export interface AdminSetting {
  id?: number;
  uuid: string;
  name: string;
  value: string;
  description?: string;
  updatedAt?: string;
}

export interface ContactMessage {
  id?: number;
  uuid?: string;
  name: string;
  email: string;
  message: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  read: boolean;
  replied: boolean;
  archived: boolean;
  createdAt: string;
}

export interface GuestPass {
  uuid: string;
  label: string;
  token?: string;
  expiresAt: string;
  revoked: boolean;
  expired: boolean;
  ownerId?: number;
  createdAt: string;
}
export interface Announcement {
  uuid: string;
  title: string;
  message: string;
  targetRole?: string;
  read?: boolean;
  createdAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size?: number;
  number?: number;
}
