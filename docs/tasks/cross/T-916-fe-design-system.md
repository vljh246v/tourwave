# T-916 — [FE] 디자인 시스템 기본 컴포넌트 (Button/Input/Modal/Card/Form)

## Meta
- ID: T-916
- Milestone: Cross-cutting
- Domain: common
- Area: FE
- Layer: design-system
- Size: L
- Depends on: 없음
- Blocks: T-008~T-027 (대부분 UI 태스크)
- GitHub Issue: —
- Status: Done

## 파일 소유권 (FE 리포 기준)
WRITE:
  - `src/components/ui/` 신규 디렉토리
    - `Button.tsx` (primary, secondary, danger variants)
    - `Input.tsx` (텍스트, 이메일, 숫자, 상태)
    - `Modal.tsx` (제목, 내용, 액션 버튼)
    - `Card.tsx` (헤더, 바디, 풋터)
    - `Form.tsx` / `FormField.tsx` (폼 그룹화)
    - `index.ts` (export)
  - `src/styles/` 또는 Tailwind 설정 (theme 확장)
  - `package.json` (shadcn/ui 검토, 또는 수동 구현)

READ:
  - `AGENTS.md` (컴포넌트 구조, 도메인 비종속 원칙)
  - Tailwind CSS v4 문서 (PostCSS 플러그인)
  - 없음 (디자인 명세는 별도 또는 기본값)

DO NOT TOUCH:
  - `src/features/` (도메인 특화 컴포넌트)
  - T-911~T-915 파일

## SSOT 근거
- 프로젝트 필수 인프라 — UI 컴포넌트 없으면 모든 화면이 ad-hoc 구현.
- **구현 방식 선택:**
  - **Option A: shadcn/ui** (권장)
    - Headless + Tailwind 기반 (스타일 완전 제어)
    - 복사-paste 방식 (의존성 추가 없음)
    - 컴포넌트 자체 코드 소유권 명확
  - **Option B: 수동 구현**
    - Tailwind만 사용 (더 가볍지만 보일러플레이트 증가)
    - 일관성 유지 어려움
  
  **선택:** Option A (shadcn/ui) 권장, 설정 가이드 포함. Option B도 가능.

## 현재 상태 (갭)
- [x] Tailwind CSS v4 설정 존재 (package.json, postcss.config.js)
- [ ] UI 컴포넌트 라이브러리 미존재 — 각 화면이 스타일을 독립적으로 정의.
- [ ] 디자인 일관성 부재 — 버튼, 입력, 모달 스타일 분산.
- [ ] 접근성(a11y) 미고려 — aria 속성, 키보드 네비게이션 부재.

## 구현 지침

### Option A: shadcn/ui (권장)

1. **shadcn/ui 초기화 (선택사항, CLI 지원)**
   ```bash
   npx shadcn-ui@latest init
   # 또는 수동: 컴포넌트 파일을 src/components/ui/로 복사
   ```

2. **기본 컴포넌트 (src/components/ui/)**

   **Button.tsx**
   ```typescript
   import React from 'react';
   
   interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
     variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
     size?: 'sm' | 'md' | 'lg';
     isLoading?: boolean;
     children: React.ReactNode;
   }
   
   export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
     ({ variant = 'primary', size = 'md', isLoading, className, ...props }, ref) => {
       const baseClasses = 'font-semibold rounded transition-colors';
       const variantClasses = {
         primary: 'bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50',
         secondary: 'bg-gray-200 text-gray-900 hover:bg-gray-300',
         danger: 'bg-red-600 text-white hover:bg-red-700',
         ghost: 'bg-transparent hover:bg-gray-100',
       };
       const sizeClasses = {
         sm: 'px-3 py-1 text-sm',
         md: 'px-4 py-2 text-base',
         lg: 'px-6 py-3 text-lg',
       };
       
       return (
         <button
           ref={ref}
           className={`${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${className || ''}`}
           disabled={isLoading || props.disabled}
           {...props}
         >
           {isLoading ? 'Loading...' : props.children}
         </button>
       );
     }
   );
   Button.displayName = 'Button';
   ```

   **Input.tsx**
   ```typescript
   import React from 'react';
   
   interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
     label?: string;
     error?: string;
     helperText?: string;
   }
   
   export const Input = React.forwardRef<HTMLInputElement, InputProps>(
     ({ label, error, helperText, className, ...props }, ref) => (
       <div className="flex flex-col gap-1">
         {label && <label className="text-sm font-medium">{label}</label>}
         <input
           ref={ref}
           className={`
             px-3 py-2 border rounded text-sm
             focus:outline-none focus:ring-2 focus:ring-blue-500
             ${error ? 'border-red-500 bg-red-50' : 'border-gray-300'}
             ${className || ''}
           `}
           {...props}
         />
         {error && <span className="text-xs text-red-600">{error}</span>}
         {helperText && <span className="text-xs text-gray-500">{helperText}</span>}
       </div>
     )
   );
   Input.displayName = 'Input';
   ```

   **Modal.tsx**
   ```typescript
   import React, { useState } from 'react';
   
   interface ModalProps {
     isOpen: boolean;
     onClose: () => void;
     title: string;
     children: React.ReactNode;
     actions?: { label: string; onClick: () => void; variant?: string }[];
   }
   
   export function Modal({ isOpen, onClose, title, children, actions }: ModalProps) {
     if (!isOpen) return null;
     
     return (
       <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
         <div className="bg-white rounded-lg shadow-lg max-w-md w-full">
           <div className="px-6 py-4 border-b">
             <h2 className="text-xl font-bold">{title}</h2>
           </div>
           <div className="px-6 py-4">{children}</div>
           {actions && (
             <div className="px-6 py-4 border-t flex gap-2 justify-end">
               {actions.map((action, i) => (
                 <button
                   key={i}
                   onClick={action.onClick}
                   className={`px-4 py-2 rounded ${
                     action.variant === 'danger'
                       ? 'bg-red-600 text-white'
                       : 'bg-gray-200'
                   }`}
                 >
                   {action.label}
                 </button>
               ))}
             </div>
           )}
         </div>
       </div>
     );
   }
   ```

   **Card.tsx**
   ```typescript
   import React from 'react';
   
   interface CardProps {
     header?: React.ReactNode;
     footer?: React.ReactNode;
     children: React.ReactNode;
     className?: string;
   }
   
   export function Card({ header, footer, children, className }: CardProps) {
     return (
       <div className={`border rounded-lg shadow-sm overflow-hidden ${className || ''}`}>
         {header && <div className="px-6 py-4 border-b bg-gray-50">{header}</div>}
         <div className="px-6 py-4">{children}</div>
         {footer && <div className="px-6 py-4 border-t bg-gray-50">{footer}</div>}
       </div>
     );
   }
   ```

   **FormField.tsx** (재사용 가능한 폼 필드)
   ```typescript
   import React from 'react';
   import { Input } from './Input';
   
   interface FormFieldProps {
     name: string;
     label: string;
     type?: string;
     value: string | number;
     onChange: (value: string | number) => void;
     error?: string;
     required?: boolean;
   }
   
   export function FormField({
     name,
     label,
     type = 'text',
     value,
     onChange,
     error,
     required,
   }: FormFieldProps) {
     return (
       <Input
         id={name}
         name={name}
         label={label}
         type={type}
         value={value}
         onChange={(e) => onChange(e.target.value)}
         error={error}
         required={required}
       />
     );
   }
   ```

   **index.ts** (export)
   ```typescript
   export { Button } from './Button';
   export { Input } from './Input';
   export { Modal } from './Modal';
   export { Card } from './Card';
   export { FormField } from './FormField';
   ```

3. **테마 확장 (선택사항, tailwind.config.ts)**
   ```typescript
   import type { Config } from 'tailwindcss';
   
   export default {
     content: [
       './src/pages/**/*.{js,ts,jsx,tsx}',
       './src/components/**/*.{js,ts,jsx,tsx}',
     ],
     theme: {
       extend: {
         colors: {
           primary: '#2563eb',
           secondary: '#64748b',
         },
       },
     },
   } satisfies Config;
   ```

### Option B: 수동 구현 (대안)
- Tailwind 유틸리티 직접 조합
- 스타일 일관성은 CSS 컨벤션 문서 필수
- shadcn/ui보다 가볍지만 유지보수 어려움

## Acceptance Criteria
- [x] `src/components/ui/` 디렉토리 생성 + 5개 컴포넌트 (Button, Input, Modal, Card, FormField)
- [x] 각 컴포넌트 TypeScript 타입 정의 명확
- [x] Button 최소 3개 variant (primary, secondary, danger)
- [x] Input label, error, helperText 지원
- [x] Modal header, body, footer + action 버튼 지원
- [x] Card header, body, footer 슬롯 지원
- [x] FormField Input 래퍼로 form 간단히 구성 가능
- [x] 모든 컴포넌트 export (index.ts)
- [x] npm run typecheck 통과
- [ ] Storybook 기초 또는 컴포넌트 예시 주석 포함 (선택)

## Verification
```bash
./scripts/verify-task.sh T-916
# 자동 검증:
# 1. src/components/ui/ 디렉토리 및 5개 파일 존재
# 2. npm run typecheck 통과
# 3. 컴포넌트 기본 import 가능 확인
# 4. Tailwind 클래스 적용 여부 (컴파일 에러 없음)
```

## Rollback
```bash
# 컴포넌트 디렉토리 제거
rm -rf src/components/ui/
# 또는 git 복구
git checkout HEAD -- src/components/ui/
```

## Notes
- **접근성 (a11y):** 모든 컴포넌트에 ARIA 속성 추가 권장.
  - Button: aria-label, aria-pressed
  - Input: aria-label, aria-required, aria-invalid
  - Modal: role="dialog", aria-labelledby, aria-modal
- **성능:** 컴포넌트는 React.memo 또는 forwardRef 활용 (불필요한 리렌더링 방지).
- **Storybook:** 나중 단계 (선택사항). 초기는 예시 주석만.
- **Tailwind v4:** PostCSS 플러그인 방식이므로 CSS-in-JS (styled-components) 혼합 금지.
- **대안:** shadcn/ui 대신 Headless UI, Radix UI, Chakra UI 등 고려 가능 (팀 선택).
- **Depends on 재검토:** T-916은 대부분 UI 태스크 Blocks. 우선 작업 권장 (T-911~T-915 병렬 가능).
- **Figma 연동:** 디자인 시스템이 있다면 Figma tokens 등으로 색상/사이즈 동기화 (나중).
