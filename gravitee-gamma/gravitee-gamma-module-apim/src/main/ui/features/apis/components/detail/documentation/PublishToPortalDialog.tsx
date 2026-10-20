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
    Alert,
    AlertDescription,
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldDescription,
    FieldLabel,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { AlertCircleIcon, InfoIcon } from '@gravitee/graphene-core/icons';
import { useId, useState, type FormEvent } from 'react';

import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useApiPublishLocations, useCreateApiOverviewPage, usePublishApiToPortal } from '../../../hooks/useApiDocumentation';
import type { ApiPortalPublication, PortalNavigationPage, PublishApiToPortal } from '../../../types/apiDocumentation';
import { defaultPublishSectionId, getHiddenListing } from '../../../utils/documentationTree';

export function PublishToPortalDialog({
    open,
    apiId,
    publications,
    hasPage,
    canCreatePage,
    onOverviewPageCreated,
    onClose,
}: Readonly<{
    open: boolean;
    apiId: string;
    /** Where the API is listed: a hidden listing is reused, and moved if needed, rather than another one added. */
    publications: ApiPortalPublication[];
    /** Listed with no page, the API would have nothing to open in the portal. */
    hasPage: boolean;
    canCreatePage: boolean;
    onOverviewPageCreated: (pageId: string) => void;
    onClose: () => void;
}>) {
    // Held here rather than in the forms: closing the dialog mid-request would unmount the form and lose a refusal.
    const publish = usePublishApiToPortal(apiId);
    const createOverviewPage = useCreateApiOverviewPage(apiId);
    const isPending = publish.isPending || createOverviewPage.isPending;

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && !isPending && onClose()}>
            <DialogContent style={{ maxWidth: '480px' }}>
                {hasPage ? (
                    <PublishToPortalForm
                        apiId={apiId}
                        publications={publications}
                        publishApi={publish.mutateAsync}
                        isPublishing={publish.isPending}
                        onClose={onClose}
                    />
                ) : (
                    <AddPageFirst
                        canCreatePage={canCreatePage}
                        createOverviewPage={() => createOverviewPage.mutateAsync()}
                        isCreating={createOverviewPage.isPending}
                        onCreated={onOverviewPageCreated}
                        onClose={onClose}
                    />
                )}
            </DialogContent>
        </Dialog>
    );
}

function AddPageFirst({
    canCreatePage,
    createOverviewPage,
    isCreating,
    onCreated,
    onClose,
}: Readonly<{
    canCreatePage: boolean;
    createOverviewPage: () => Promise<PortalNavigationPage>;
    isCreating: boolean;
    onCreated: (pageId: string) => void;
    onClose: () => void;
}>) {
    const [refusal, setRefusal] = useState<string | null>(null);

    async function handleCreate() {
        setRefusal(null);
        try {
            const page = await createOverviewPage();
            notify.success(`Page '${page.title}' created`);
            onCreated(page.id);
        } catch (error) {
            setRefusal(extractErrorMessage(error));
        }
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Add a page before publishing</DialogTitle>
                <DialogDescription>
                    An API needs at least one page to be shown in the developer portal.{' '}
                    {canCreatePage
                        ? "Start from an Overview page built from the API's details."
                        : 'Ask someone who can add documentation to this API.'}
                </DialogDescription>
            </DialogHeader>

            {refusal ? (
                <Alert variant="destructive">
                    <AlertCircleIcon aria-hidden />
                    <AlertDescription>{refusal}</AlertDescription>
                </Alert>
            ) : null}

            <DialogFooter>
                {canCreatePage ? (
                    <>
                        <Button type="button" variant="outline" onClick={onClose} disabled={isCreating}>
                            Cancel
                        </Button>
                        <Button type="button" onClick={() => void handleCreate()} disabled={isCreating}>
                            {isCreating ? 'Creating…' : 'Create Overview page'}
                        </Button>
                    </>
                ) : (
                    <Button type="button" variant="outline" onClick={onClose}>
                        Close
                    </Button>
                )}
            </DialogFooter>
        </>
    );
}

// Rendered inside the dialog content, so its state starts over each time the dialog opens.
function PublishToPortalForm({
    apiId,
    publications,
    publishApi,
    isPublishing,
    onClose,
}: Readonly<{
    apiId: string;
    publications: ApiPortalPublication[];
    publishApi: (request: PublishApiToPortal) => Promise<ApiPortalPublication>;
    isPublishing: boolean;
    onClose: () => void;
}>) {
    const sectionId = useId();
    const locationsQuery = useApiPublishLocations(apiId);
    const [chosenSectionId, setChosenSectionId] = useState<string | undefined>(undefined);
    const [refusal, setRefusal] = useState<string | null>(null);

    const locations = locationsQuery.data?.data ?? [];
    const selectedSectionId = chosenSectionId ?? defaultPublishSectionId(locations, publications);
    const hiddenListing = getHiddenListing(publications);
    const hiddenListingMoves =
        locationsQuery.isSuccess &&
        hiddenListing !== undefined &&
        !locations.some(location => location.id === hiddenListing.portalNavigationItem.parentId);
    const hasNoSection = locationsQuery.isSuccess && locations.length === 0;
    const canPublish = selectedSectionId !== undefined && !isPublishing;

    function handleSectionChange(next: string) {
        // Inside a form, the select's hidden native twin reports an empty value before the sections arrive.
        if (next === '') return;
        setChosenSectionId(next);
        setRefusal(null);
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canPublish) return;
        try {
            const published = await publishApi({ sectionId: selectedSectionId });
            notify.success(`Published in '${published.sectionName}'`);
            onClose();
        } catch (error) {
            setRefusal(extractErrorMessage(error));
        }
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Publish to the developer portal</DialogTitle>
                <DialogDescription>
                    The API and its documentation will become visible in the developer portal. Every page, folder and link will be
                    published, including those unpublished individually.
                </DialogDescription>
            </DialogHeader>

            <form id="publish-api-to-portal-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                {hiddenListingMoves ? (
                    <Alert>
                        <InfoIcon aria-hidden />
                        <AlertDescription>
                            This API is already in the portal menu, hidden, under &quot;{hiddenListing.sectionName}&quot;. Publishing moves
                            it to the section you choose.
                        </AlertDescription>
                    </Alert>
                ) : null}

                {locationsQuery.isError ? (
                    <Alert variant="destructive">
                        <AlertCircleIcon aria-hidden />
                        <AlertDescription>Failed to load the portal sections. Close and try again.</AlertDescription>
                    </Alert>
                ) : null}

                {refusal ? (
                    <Alert variant="destructive">
                        <AlertCircleIcon aria-hidden />
                        <AlertDescription>{refusal}</AlertDescription>
                    </Alert>
                ) : null}

                <Field orientation="vertical" className="gap-1.5">
                    <FieldLabel htmlFor={sectionId}>Section</FieldLabel>
                    <Select
                        value={selectedSectionId ?? ''}
                        onValueChange={handleSectionChange}
                        disabled={locations.length === 0 || isPublishing}
                    >
                        <SelectTrigger id={sectionId} className="w-full">
                            <SelectValue placeholder={sectionPlaceholder(locationsQuery.isPending, hasNoSection)} />
                        </SelectTrigger>
                        {/* Aligned on the selected item, the list is placed against the page rather than the dialog */}
                        <SelectContent position="popper" style={{ width: 'var(--radix-select-trigger-width)', minWidth: 'unset' }}>
                            {locations.map(location => (
                                <SelectItem key={location.id} value={location.id}>
                                    {location.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                    {hasNoSection ? (
                        <FieldDescription>
                            The portal&apos;s top navigation has no published section to list this API in. Ask a portal administrator to add
                            one.
                        </FieldDescription>
                    ) : null}
                </Field>
            </form>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isPublishing}>
                    Cancel
                </Button>
                <Button type="submit" form="publish-api-to-portal-form" disabled={!canPublish}>
                    {isPublishing ? 'Publishing…' : 'Publish'}
                </Button>
            </DialogFooter>
        </>
    );
}

function sectionPlaceholder(isLoading: boolean, hasNoSection: boolean): string {
    if (isLoading) return 'Loading sections…';
    if (hasNoSection) return 'No sections available';
    return 'Choose a section';
}
