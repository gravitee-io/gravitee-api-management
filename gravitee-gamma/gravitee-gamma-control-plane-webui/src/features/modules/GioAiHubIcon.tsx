/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { forwardRef, useId } from 'react';
import type { SVGProps } from 'react';

/** AI Hub product icon, drawn like the Graphene product icons: a hub and the models and tools around it. */
export const GioAiHubIcon = forwardRef<SVGSVGElement, SVGProps<SVGSVGElement>>((props, ref) => {
    const id = useId();
    return (
        <svg ref={ref} xmlns="http://www.w3.org/2000/svg" width={24} height={24} viewBox="0 0 720 720" fill="none" {...props}>
            <g className="dark:hidden">
                <circle cx="360" cy="360" r="150" fill={`url(#${id}-l0)`} />
                <circle cx="360.0" cy="102.0" r="78" fill={`url(#${id}-l1)`} />
                <circle cx="583.43" cy="231.0" r="78" fill={`url(#${id}-l2)`} />
                <circle cx="583.43" cy="489.0" r="78" fill={`url(#${id}-l3)`} />
                <circle cx="360.0" cy="618.0" r="78" fill={`url(#${id}-l4)`} />
                <circle cx="136.57" cy="489.0" r="78" fill={`url(#${id}-l5)`} />
                <circle cx="136.57" cy="231.0" r="78" fill={`url(#${id}-l6)`} />
                <defs>
                    <linearGradient id={`${id}-l0`} x1="210" y1="360" x2="510" y2="360" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.51" stopColor="#F31013" />
                        <stop offset="1" stopColor="#FF8A00" />
                    </linearGradient>
                    <linearGradient id={`${id}-l1`} x1="438.0" y1="102.0" x2="282.0" y2="102.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.51" stopColor="#F31013" />
                        <stop offset="1" stopColor="#FF8A00" />
                    </linearGradient>
                    <linearGradient
                        id={`${id}-l2`}
                        x1="505.42999999999995"
                        y1="231.0"
                        x2="661.43"
                        y2="231.0"
                        gradientUnits="userSpaceOnUse"
                    >
                        <stop stopColor="#410A00" />
                        <stop offset="0.51" stopColor="#F31013" />
                        <stop offset="1" stopColor="#FF8A00" />
                    </linearGradient>
                    <linearGradient
                        id={`${id}-l3`}
                        x1="661.43"
                        y1="489.0"
                        x2="505.42999999999995"
                        y2="489.0"
                        gradientUnits="userSpaceOnUse"
                    >
                        <stop stopColor="#410A00" />
                        <stop offset="0.51" stopColor="#F31013" />
                        <stop offset="1" stopColor="#FF8A00" />
                    </linearGradient>
                    <linearGradient id={`${id}-l4`} x1="282.0" y1="618.0" x2="438.0" y2="618.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.51" stopColor="#F31013" />
                        <stop offset="1" stopColor="#FF8A00" />
                    </linearGradient>
                    <linearGradient id={`${id}-l5`} x1="214.57" y1="489.0" x2="58.56999999999999" y2="489.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.51" stopColor="#F31013" />
                        <stop offset="1" stopColor="#FF8A00" />
                    </linearGradient>
                    <linearGradient id={`${id}-l6`} x1="58.56999999999999" y1="231.0" x2="214.57" y2="231.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.51" stopColor="#F31013" />
                        <stop offset="1" stopColor="#FF8A00" />
                    </linearGradient>
                </defs>
            </g>
            <g className="hidden dark:block">
                <circle cx="360" cy="360" r="150" fill={`url(#${id}-d0)`} />
                <circle cx="360.0" cy="102.0" r="78" fill={`url(#${id}-d1)`} />
                <circle cx="583.43" cy="231.0" r="78" fill={`url(#${id}-d2)`} />
                <circle cx="583.43" cy="489.0" r="78" fill={`url(#${id}-d3)`} />
                <circle cx="360.0" cy="618.0" r="78" fill={`url(#${id}-d4)`} />
                <circle cx="136.57" cy="489.0" r="78" fill={`url(#${id}-d5)`} />
                <circle cx="136.57" cy="231.0" r="78" fill={`url(#${id}-d6)`} />
                <defs>
                    <linearGradient id={`${id}-d0`} x1="210" y1="360" x2="510" y2="360" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.37" stopColor="#F31013" />
                        <stop offset="0.75" stopColor="#FF8A00" />
                        <stop offset="1" stopColor="white" />
                    </linearGradient>
                    <linearGradient id={`${id}-d1`} x1="438.0" y1="102.0" x2="282.0" y2="102.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.37" stopColor="#F31013" />
                        <stop offset="0.75" stopColor="#FF8A00" />
                        <stop offset="1" stopColor="white" />
                    </linearGradient>
                    <linearGradient
                        id={`${id}-d2`}
                        x1="505.42999999999995"
                        y1="231.0"
                        x2="661.43"
                        y2="231.0"
                        gradientUnits="userSpaceOnUse"
                    >
                        <stop stopColor="#410A00" />
                        <stop offset="0.37" stopColor="#F31013" />
                        <stop offset="0.75" stopColor="#FF8A00" />
                        <stop offset="1" stopColor="white" />
                    </linearGradient>
                    <linearGradient
                        id={`${id}-d3`}
                        x1="661.43"
                        y1="489.0"
                        x2="505.42999999999995"
                        y2="489.0"
                        gradientUnits="userSpaceOnUse"
                    >
                        <stop stopColor="#410A00" />
                        <stop offset="0.37" stopColor="#F31013" />
                        <stop offset="0.75" stopColor="#FF8A00" />
                        <stop offset="1" stopColor="white" />
                    </linearGradient>
                    <linearGradient id={`${id}-d4`} x1="282.0" y1="618.0" x2="438.0" y2="618.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.37" stopColor="#F31013" />
                        <stop offset="0.75" stopColor="#FF8A00" />
                        <stop offset="1" stopColor="white" />
                    </linearGradient>
                    <linearGradient id={`${id}-d5`} x1="214.57" y1="489.0" x2="58.56999999999999" y2="489.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.37" stopColor="#F31013" />
                        <stop offset="0.75" stopColor="#FF8A00" />
                        <stop offset="1" stopColor="white" />
                    </linearGradient>
                    <linearGradient id={`${id}-d6`} x1="58.56999999999999" y1="231.0" x2="214.57" y2="231.0" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#410A00" />
                        <stop offset="0.37" stopColor="#F31013" />
                        <stop offset="0.75" stopColor="#FF8A00" />
                        <stop offset="1" stopColor="white" />
                    </linearGradient>
                </defs>
            </g>
        </svg>
    );
});
GioAiHubIcon.displayName = 'GioAiHubIcon';
