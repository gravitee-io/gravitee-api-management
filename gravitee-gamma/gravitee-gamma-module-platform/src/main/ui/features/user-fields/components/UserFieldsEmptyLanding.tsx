/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    ArrowRightIcon,
    CircleCheckIcon,
    CircleXIcon,
    FileTextIcon,
    ListIcon,
    UserIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../../shared/components';

const WITHOUT_FIELDS = [
    'Registration only asks for email, first name, and last name',
    'No way to capture department, country, or job title',
    'You cannot make an extra question required',
] as const;

const WITH_FIELDS = [
    'Add the questions your organization actually needs',
    'Mark a field required so registration cannot skip it',
    'Offer a fixed list of answers when free text would drift',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: UserIcon,
        title: 'Ask on registration',
        description: 'Each field appears on APIM console and developer portal sign-up, next to the built-in name and email.',
    },
    {
        Icon: FileTextIcon,
        title: 'Label it for people',
        description: 'The key is stored on the profile. The label is what the registrant reads.',
    },
    {
        Icon: ListIcon,
        title: 'Constrain the answers',
        description: 'Leave values empty for free text, or add a list so the field becomes a choice.',
    },
];

function FlowNode({ Icon, label }: { Icon: LucideIcon; label: string }) {
    return (
        <div className="flex flex-col items-center text-center">
            <div className="rounded-lg bg-muted p-2">
                <Icon className="size-4 text-muted-foreground" aria-hidden />
            </div>
            <p className="mt-1 text-xs font-medium">{label}</p>
        </div>
    );
}

function ComparisonLine({ label, variant }: { label: string; variant: 'positive' | 'negative' }) {
    return (
        <li className="flex items-center gap-1 text-xs text-muted-foreground">
            {variant === 'positive' ? (
                <CircleCheckIcon className="size-3 shrink-0 text-success" aria-hidden />
            ) : (
                <CircleXIcon className="size-3 shrink-0 text-destructive" aria-hidden />
            )}
            {label}
        </li>
    );
}

export function UserFieldsEmptyLanding() {
    return (
        <div className="space-y-6 rounded-xl border p-4">
            <div>
                <h2 className="text-base font-semibold">Why create a user field?</h2>
                <p className="mt-1 text-xs text-muted-foreground">
                    Email, first name, and last name are already on APIM console and developer portal sign-up. A user field is anything else
                    you want to collect — department, country, job title — and store on the profile. Create the first field, then decide
                    whether it is required and whether answers should come from a list.
                </p>
            </div>

            <div className="flex flex-col items-stretch gap-4 md:flex-row">
                <div className="flex-1 space-y-3 rounded-xl border p-4">
                    <p className="text-xs font-semibold text-muted-foreground">Without user fields</p>
                    <div className="flex items-center justify-center gap-2">
                        <FlowNode Icon={UserIcon} label="Registrant" />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode Icon={FileTextIcon} label="Name & email only" />
                    </div>
                    <ul className="space-y-1">
                        {WITHOUT_FIELDS.map(label => (
                            <ComparisonLine key={label} label={label} variant="negative" />
                        ))}
                    </ul>
                </div>

                <div className="flex shrink-0 items-center justify-center">
                    <ArrowRightIcon className="size-5 rotate-90 text-primary md:rotate-0" aria-hidden />
                </div>

                <div className="flex-1 space-y-3 rounded-xl border border-primary/20 bg-primary/5 p-4">
                    <p className="text-xs font-semibold text-primary">With user fields</p>
                    <div className="flex items-center justify-center gap-2">
                        <FlowNode Icon={UserIcon} label="Registrant" />
                        <ArrowRightIcon className="size-4 shrink-0 text-primary/70" aria-hidden />
                        <FlowNode Icon={ListIcon} label="Your questions" />
                        <ArrowRightIcon className="size-4 shrink-0 text-primary/70" aria-hidden />
                        <FlowNode Icon={FileTextIcon} label="Richer profile" />
                    </div>
                    <ul className="space-y-1">
                        {WITH_FIELDS.map(label => (
                            <ComparisonLine key={label} label={label} variant="positive" />
                        ))}
                    </ul>
                </div>
            </div>

            <div className="flex flex-col gap-4 border-t pt-5 md:flex-row">
                {FEATURE_TILES.map(({ Icon, title, description }) => (
                    <div key={title} className="flex-1">
                        <FeatureTile Icon={Icon} title={title} description={description} />
                    </div>
                ))}
            </div>
        </div>
    );
}
