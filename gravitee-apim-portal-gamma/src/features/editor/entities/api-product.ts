export interface ApiProduct {
    id: string;
    name: string;
    version: string;
    description: string;
    apiIds?: string[];
}

export interface ApiProductsResponse {
    data?: ApiProduct[];
    metadata?: {
        pagination?: {
            current_page?: number;
            size?: number;
            total?: number;
            total_pages?: number;
        };
    };
}
