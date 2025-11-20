<gmd-grid>
    <gmd-md class="homepage-title">
        # Welcome to the Developer Portal
        Access all APIs, documentation, and tools to build your next integration.
    </gmd-md>
    <gmd-cell style="text-align: center; margin: auto;">
        <gmd-button link="/catalog">Explore all APIs</gmd-button>
        <gmd-button link="/guides" appearance="outlined" style="--gmd-button-outlined-label-text-weight: 700; --gmd-button-outlined-label-text-color: black;"
        >Get started</gmd-button>
    </gmd-cell>
    <img class="homepage-cover-photo" src="assets/homepage/desk.png" title="Homepage picture"/>
</gmd-grid>

### Your toolkit for building

<gmd-grid columns="3">
    <gmd-md>
        ![book](./assets/homepage/book.svg "Book icon")
        #### API catalog
        Browse and test all available APIs in one place.
    </gmd-md>
    <gmd-md>
        ![laptop](./assets/homepage/laptop.svg "Laptop icon")
        #### Interactive docs
        Explore clear, structured documentation with code samples.
    </gmd-md>
    <gmd-md>
        ![vector](./assets/homepage/vector.svg "Vector icon")
        #### Usage analytics
        Track API usage, error rates, and performance metrics.
    </gmd-md>
    <gmd-md>
        ![group](./assets/homepage/group.svg "Group icon")
        #### API catalog
        Browse and test all available APIs in one place.
    </gmd-md>
    <gmd-md>
        ![support](./assets/homepage/support.svg "Support icon")
        #### Interactive docs
        Explore clear, structured documentation with code samples.
    </gmd-md>
    <gmd-md>
        ![support](./assets/homepage/service.svg "Service icon")
        #### Usage analytics
        Track API usage, error rates, and performance metrics.
    </gmd-md>
</gmd-grid>

### Get started in minutes

<gmd-grid columns="3">
    <gmd-card backgroundColor="none">
        <gmd-card-title>Your first API call</gmd-card-title>
        <gmd-md>Learn how to make a basic request and receive a response.Learn how to make a basic request and receive a response.</gmd-md>
        <div class="flex-container">
            <gmd-button link="/guides" appearance="outlined" class="get-started-card__button"
            >Read <img src="assets/homepage/arrow-right.svg" alt="arrow right icon" title="Arrow right icon"/></gmd-button>
        </div>
    </gmd-card>
    <gmd-card backgroundColor="none">
        <gmd-card-title>Authentication walkthrough</gmd-card-title>
            <gmd-md>A step-by-step guide to generating and managing API keys.</gmd-md>
            <div class="flex-container">
                <gmd-button link="/guides" appearance="outlined" class="get-started-card__button"
                >Read <img src="assets/homepage/arrow-right.svg" alt="arrow right icon" title="Arrow right icon"/></gmd-button>
            </div>
        </gmd-card>
    <gmd-card backgroundColor="none">
        <gmd-card-title>Integrating SDK into your project</gmd-card-title>
        <gmd-md>Use our official library to simplify your code.</gmd-md>
        <div class="flex-container">
            <gmd-button link="/guides" appearance="outlined" class="get-started-card__button"
            >Read <img src="assets/homepage/arrow-right.svg" alt="arrow right icon" title="Arrow right icon"/></gmd-button>
        </div>
    </gmd-card>
</gmd-grid>
<style>
  .homepage-title {
    display: flex;
    flex-direction: column;
    max-width: 100%;
    text-align: center;
    margin: auto;
  }

  .homepage-cover-photo {
    display: flex;
    max-width: 100%;
    margin: 80px auto;
  }
  
  .get-started-card__button {
    --gmd-button-outlined-label-text-weight: 700;
    --gmd-button-outlined-label-text-color: black;
    margin-top: auto;
    padding-top: 12px;
  }

  .flex-container {
    display: flex;
    flex-direction: column;
    height: 100%
  }
</style>